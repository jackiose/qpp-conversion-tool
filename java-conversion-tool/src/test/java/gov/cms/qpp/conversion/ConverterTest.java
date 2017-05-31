package gov.cms.qpp.conversion;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.qpp.conversion.decode.XmlInputFileException;
import gov.cms.qpp.conversion.encode.EncodeException;
import gov.cms.qpp.conversion.encode.QppOutputEncoder;
import gov.cms.qpp.conversion.model.AnnotationMockHelper;
import gov.cms.qpp.conversion.model.TemplateId;
import gov.cms.qpp.conversion.model.error.AllErrors;
import gov.cms.qpp.conversion.stubs.JennyDecoder;
import gov.cms.qpp.conversion.stubs.TestDefaultValidator;
import gov.cms.qpp.conversion.validate.QrdaValidator;
import gov.cms.qpp.conversion.xml.XmlException;
import gov.cms.qpp.conversion.xml.XmlUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "org.apache.xerces.*", "javax.xml.parsers.*", "org.xml.sax.*" })
public class ConverterTest {

	@After
	public void cleanup() throws IOException {
		Files.deleteIfExists(Paths.get("defaultedNode.qpp.json"));
		Files.deleteIfExists(Paths.get("defaultedNode.err.json"));
		Files.deleteIfExists(Paths.get("non-xml-file.err.json"));
		Files.deleteIfExists(Paths.get("not-a-QRDA-III-file.err.json"));
	}

	@Test
	@PrepareForTest({Converter.class, QrdaValidator.class})
	public void testValidationErrors() throws Exception {

		//mocking
		AnnotationMockHelper.mockDecoder(TemplateId.DEFAULT, JennyDecoder.class);
		QrdaValidator mockQrdaValidator = AnnotationMockHelper.mockValidator(TemplateId.DEFAULT, TestDefaultValidator.class, true);
		PowerMockito.whenNew(QrdaValidator.class).withNoArguments().thenReturn(mockQrdaValidator);

		//set-up
		Path defaultJson = Paths.get("errantDefaultedNode.qpp.json");
		Path defaultError = Paths.get("errantDefaultedNode.err.json");

		Files.deleteIfExists(defaultJson);
		Files.deleteIfExists(defaultError);

		//execute
		Path path = Paths.get("src/test/resources/converter/errantDefaultedNode.xml");
		new Converter(path).transform();

		//assert
		assertThat("The JSON file must not exist", Files.exists(defaultJson), is(false));
		assertThat("The error file must exist", Files.exists(defaultError), is(true));

		String errorContent = new String(Files.readAllBytes(defaultError));
		assertThat("The error file is missing the specified content", errorContent, containsString("Jenny"));

		//clean-up
		Files.delete(defaultError);
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class})
	public void testInvalidXml() throws IOException {

		//set-up
		mockStatic( LoggerFactory.class );
		Logger devLogger = mock( Logger.class );
		Logger clientLogger = mock( Logger.class );
		when( LoggerFactory.getLogger(any(Class.class)) ).thenReturn( devLogger );
		when( LoggerFactory.getLogger(anyString()) ).thenReturn( clientLogger );

		//execute
		Path path = Paths.get("src/test/resources/non-xml-file.xml");
		new Converter(path).transform();

		Path errOutputPath = Paths.get("non-xml-file.err.json");
		String errorOutput = new String(Files.readAllBytes(errOutputPath));

		//assert
		verify(clientLogger).error( eq(Converter.NOT_VALID_XML_DOCUMENT) );
		assertThat("File must contain error message", errorOutput, containsString(Converter.NOT_VALID_XML_DOCUMENT));
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class, QppOutputEncoder.class})
	public void testEncodingExceptions() throws Exception {

		//set-up
		mockStatic( LoggerFactory.class );
		Logger devLogger = mock( Logger.class );
		Logger clientLogger = mock( Logger.class );
		when( LoggerFactory.getLogger(any(Class.class)) ).thenReturn( devLogger );
		when( LoggerFactory.getLogger(anyString()) ).thenReturn( clientLogger );

		QppOutputEncoder encoder = mock( QppOutputEncoder.class );
		whenNew( QppOutputEncoder.class ).withNoArguments().thenReturn( encoder );
		EncodeException ex = new EncodeException( "mocked", new RuntimeException() );
		doThrow( ex ).when( encoder ).encode( any(Writer.class) );

		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		new Converter(path)
				.doDefaults(false)
				.doValidation(false)
				.transform();

		//assert
		verify(devLogger).error( eq(Converter.NOT_VALID_XML_DOCUMENT), any(XmlException.class));
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class, Files.class})
	public void testIOEncodingError() throws Exception {

		//set-up
		stub(method(Files.class, "newBufferedWriter", Path.class, OpenOption.class)).toThrow( new IOException() );

		mockStatic( LoggerFactory.class );
		Logger devLogger = mock( Logger.class );
		Logger clientLogger = mock( Logger.class );
		when( LoggerFactory.getLogger(any(Class.class)) ).thenReturn( devLogger );
		when( LoggerFactory.getLogger(anyString()) ).thenReturn( clientLogger );

		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		new Converter(path)
				.doDefaults(false)
				.doValidation(false)
				.transform();

		//assert
		verify(devLogger).error( eq(Converter.NOT_VALID_XML_DOCUMENT),
				any(XmlInputFileException.class) );
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class, Files.class})
	public void testUnexpectedEncodingError() throws Exception {

		//set-up
		mockStatic(Files.class);
		when(Files.newBufferedWriter(any(Path.class))).thenReturn(null).thenCallRealMethod();
		when(Files.readAllBytes(any(Path.class))).thenCallRealMethod();

		mockStatic( LoggerFactory.class );
		Logger devLogger = mock( Logger.class );
		Logger clientLogger = mock( Logger.class );
		when( LoggerFactory.getLogger(any(Class.class)) ).thenReturn( devLogger );
		when( LoggerFactory.getLogger(anyString()) ).thenReturn( clientLogger );

		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		new Converter(path)
				.doDefaults(false)
				.doValidation(false)
				.transform();

		Path errOutputPath = Paths.get("defaultedNode.err.json");
		String errorOutput = new String(Files.readAllBytes(errOutputPath));

		//assert
		verify(devLogger).error( eq(Converter.UNEXPECTED_ERROR), any(NullPointerException.class) );
		assertThat("File must contain error message", errorOutput, containsString(Converter.UNEXPECTED_ERROR));
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class, Files.class})
	public void testExceptionOnWriterClose() throws Exception {

		//set-up
		BufferedWriter writer = mock( BufferedWriter.class );
		doThrow( new IOException() ).when( writer ).close();
		mockStatic(Files.class);
		when(Files.newBufferedWriter(any(Path.class))).thenReturn(writer).thenCallRealMethod();

		mockStatic( LoggerFactory.class );
		Logger devLogger = mock( Logger.class );
		Logger clientLogger = mock( Logger.class );
		when( LoggerFactory.getLogger(any(Class.class)) ).thenReturn( devLogger );
		when( LoggerFactory.getLogger(anyString()) ).thenReturn( clientLogger );

		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		new Converter(path)
				.doDefaults(false)
				.doValidation(false)
				.transform();

		//assert
		verify(devLogger).error( eq("The file is not a valid XML document"),
				any(XmlInputFileException.class) );
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class, FileWriter.class})
	public void testValidationErrorWriterInstantiation() throws Exception {

		//set-up
		stub(method(Files.class, "newBufferedWriter", Path.class, OpenOption.class)).toThrow( new IOException() );

		mockStatic(LoggerFactory.class);
		Logger devLogger = mock(Logger.class);
		Logger clientLogger = mock(Logger.class);
		when(LoggerFactory.getLogger(any(Class.class))).thenReturn(devLogger);
		when(LoggerFactory.getLogger(anyString())).thenReturn(clientLogger);

		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		new Converter(path).transform();

		//assert
		verify(devLogger).error( eq("Could not write to error file defaultedNode.err.json" ),
				any(IOException.class));
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class, FileWriter.class})
	public void testValidationErrorWriterInstantiationNull() throws Exception {

		//set-up
		stub(method(Files.class, "newBufferedWriter", Path.class, OpenOption.class)).toThrow(new IOException());

		mockStatic(LoggerFactory.class);
		Logger devLogger = mock(Logger.class);
		Logger clientLogger = mock(Logger.class);
		when(LoggerFactory.getLogger(any(Class.class))).thenReturn(devLogger);
		when(LoggerFactory.getLogger(anyString())).thenReturn(clientLogger);

		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		new Converter(path).transform();

		//assert
		verify(devLogger).error( eq("Could not write to error file defaultedNode.err.json"), any(NullPointerException.class) );
	}

	@Test
	@PrepareForTest({LoggerFactory.class, Converter.class})
	public void testExceptionOnWriteValidationErrors() throws Exception {
		mockStatic(LoggerFactory.class);
		Logger devLogger = mock(Logger.class);
		Logger clientLogger = mock(Logger.class);
		when(LoggerFactory.getLogger(any(Class.class))).thenReturn(devLogger);
		when(LoggerFactory.getLogger(anyString())).thenReturn(clientLogger);
		
		//execute
		Path path = Paths.get("src/test/resources/converter/defaultedNode.xml");
		
		Converter converter = spy(new Converter(path));
		doThrow(new IOException()).when(converter, "writeErrorJson", any(AllErrors.class), any(Writer.class));
		converter.transform();

		//assert
		verify(devLogger).error(eq("Could not write to error file defaultedNode.err.json"), any(NullPointerException.class));
	}

	@Test
	public void testInvalidXmlFile() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Converter converter = new Converter(Paths.get("src/test/resources/not-a-QRDA-III-file.xml"));

		Method transformMethod = Converter.class.getMethod("transform");
		transformMethod.setAccessible(true);
		TransformationStatus returnValue = (TransformationStatus) transformMethod.invoke(converter);

		assertThat("Should not have a valid clinical document template id", returnValue, is(TransformationStatus.NON_RECOVERABLE));
	}

	@Test
	public void testJsonCreation() throws IOException {
		Converter converter = new Converter(XmlUtils.fileToStream(Paths.get("src/test/resources/qrda_bad_denominator.xml")));

		TransformationStatus returnValue = converter.transform();

		assertThat("A non-zero return value was expected.", returnValue, is(not(TransformationStatus.SUCCESS)));

		InputStream errorResultsStream = converter.getConversionResult();
		String errorResults = IOUtils.toString(errorResultsStream, StandardCharsets.UTF_8);

		assertThat("The error results must have the source identifier.", errorResults, containsString("sourceIdentifier"));
		assertThat("The error results must have some error text.", errorResults, containsString("errorText"));
		assertThat("The error results must have an XPath.", errorResults, containsString("path"));
	}

	@Test
	@PrepareForTest({Converter.class, ObjectMapper.class})
	public void testJsonStreamFailure() throws Exception {
		//mock
		whenNew(ObjectMapper.class).withNoArguments().thenThrow(new JsonGenerationException("test exception", (JsonGenerator)null));

		//run
		Converter converter = new Converter(XmlUtils.fileToStream(Paths.get("src/test/resources/qrda_bad_denominator.xml")));
		TransformationStatus returnValue = converter.transform();

		//assert
		assertThat("A failure was expected.", returnValue, is(not(TransformationStatus.SUCCESS)));
		String expectedExceptionJson = "{ \"exception\": \"JsonProcessingException\" }";
		InputStream errorResultsStream = converter.getConversionResult();
		String errorResults = IOUtils.toString(errorResultsStream, StandardCharsets.UTF_8);

		assertThat("An exception creating the JSON should have been thrown resulting in a basic error JSON being returned.",
			expectedExceptionJson, is(errorResults));
	}

	@Test
	public void testNotAValidQrdaIIIFile() throws IOException {
		Path errOutput = Paths.get("not-a-QRDA-III-file.err.json");

		Path path = Paths.get("src/test/resources/not-a-QRDA-III-file.xml");
		new Converter(path)
				.doDefaults(false)
				.doValidation(false)
				.transform();

		String errorContent = new String(Files.readAllBytes(errOutput));

		assertThat("File must contain the error string", errorContent,
				containsString("The file is not a QRDA-III XML document"));
	}
}
