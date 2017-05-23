package gov.cms.qpp.conversion.validate;

import gov.cms.qpp.conversion.model.Node;
import gov.cms.qpp.conversion.model.TemplateId;
import gov.cms.qpp.conversion.model.error.ValidationError;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static gov.cms.qpp.conversion.model.error.ValidationErrorMatcher.containsValidationErrorInAnyOrderIgnoringPath;
import static gov.cms.qpp.conversion.model.error.ValidationErrorMatcher.validationErrorTextMatches;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

/**
 * This will test the Checker functionality
 */
public class CheckerTest {

	private static final String PARENT = "parent";
	private static final String VALUE = "value";
	private static final String ERROR_MESSAGE = "error message";
	private static final String OTHER_ERROR_MESSAGE = "some other error message";

	private List<ValidationError> validationErrors;

	@Before
	public void beforeEach() {
		validationErrors = new ArrayList<>();
	}

	@Test
	public void testValueFindFailure() {
		Node meepNode = new Node(PARENT);

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE);

		assertThat("There's an error", validationErrors, hasSize(1));
		assertThat("message applied is the message given", validationErrors.get(0),
				validationErrorTextMatches(ERROR_MESSAGE));
	}

	@Test
	public void testParentFailure() {
		Node meepNode = new Node(PARENT);

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.hasParent(ERROR_MESSAGE, TemplateId.ACI_DENOMINATOR) //fails
				.hasParent(ERROR_MESSAGE, TemplateId.ACI_DENOMINATOR); //shortcuts

		assertThat("There's an error", validationErrors, hasSize(1));
		assertThat("message applied is the message given", validationErrors.get(0),
				validationErrorTextMatches(ERROR_MESSAGE));
	}

	@Test
	public void testValueFindSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "Bob");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE);
		assertThat("There's no error", validationErrors, empty());
	}

	@Test
	public void testIntValueFindFailure() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "Bob");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.intValue(ERROR_MESSAGE, VALUE);

		assertThat("There's an error", validationErrors, hasSize(1));
	}

	@Test
	public void testIntValueFindSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE);

		assertThat("There's no error", validationErrors, empty());
	}

	@Test
	public void testChildrenFindFailure() {
		Node meepNode = new Node(PARENT);

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.hasChildren(ERROR_MESSAGE);

		assertThat ("There's an error", validationErrors, hasSize(1));
	}

	@Test
	public void testChildrenFindSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.addChildNode(new Node(TemplateId.PLACEHOLDER.getTemplateId()));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.hasChildren(ERROR_MESSAGE);

		assertThat("There's no error", validationErrors, empty());
	}

	@Test
	public void testChildrenMinimumFailure() {
		String templateId = TemplateId.PLACEHOLDER.getTemplateId();
		Node meepNode = new Node(PARENT);
		meepNode.addChildNode(new Node(templateId));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.childMinimum(ERROR_MESSAGE, 2, TemplateId.PLACEHOLDER);

		assertThat("There's an error", validationErrors, hasSize(1));
	}

	@Test
	public void testChildrenMinimumSuccess() {
		String templateId = TemplateId.PLACEHOLDER.getTemplateId();
		Node meepNode = new Node(PARENT);
		meepNode.addChildNodes(new Node(templateId), new Node(templateId));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.childMinimum(ERROR_MESSAGE, 2, TemplateId.PLACEHOLDER);

		assertThat("There's no error", validationErrors, empty());
	}

	@Test
	public void testChildrenMaximumFailure() {
		String templateId = TemplateId.PLACEHOLDER.getTemplateId();
		Node meepNode = new Node(PARENT);
		meepNode.addChildNodes(new Node(templateId),
				new Node(templateId),
				new Node(templateId));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.childMaximum(ERROR_MESSAGE, 2, TemplateId.PLACEHOLDER);

		assertThat("There's an error", validationErrors, hasSize(1));
	}

	@Test
	public void testChildrenMaximumSuccess() {
		String templateId = TemplateId.PLACEHOLDER.getTemplateId();
		Node meepNode = new Node(PARENT);
		meepNode.addChildNodes(new Node(templateId),
				new Node(templateId));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.childMaximum(ERROR_MESSAGE, 2, TemplateId.PLACEHOLDER);

		assertThat("There's no error", validationErrors, empty());
	}

	//chaining
	@Test
	public void testValueChildrenFindFailure() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE).hasChildren(OTHER_ERROR_MESSAGE);

		assertThat("There's an error", validationErrors, hasSize(1));
		assertThat("message applied is other error message", validationErrors.get(0),
				validationErrorTextMatches(OTHER_ERROR_MESSAGE));
	}

	@Test
	public void testValueChildrenFindSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");
		meepNode.addChildNode(new Node(TemplateId.PLACEHOLDER.getTemplateId()));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE).hasChildren(OTHER_ERROR_MESSAGE);

		assertThat("There's no error", validationErrors, empty());
	}

	@Test
	public void testChildValueChildrenFindFailure() {
		Node meepNode = new Node(PARENT);

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.intValue(ERROR_MESSAGE, VALUE)
				.value(ERROR_MESSAGE, VALUE)
				.childMaximum(ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER)
				.hasChildren(OTHER_ERROR_MESSAGE);
		assertThat("There's an error", validationErrors, hasSize(1));
		assertThat("message applied is other error message", validationErrors.get(0),
				validationErrorTextMatches(ERROR_MESSAGE));
	}

	@Test
	public void testValueChildrenChildMinChildMaxFindFailure() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");
		meepNode.addChildNodes(
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.PLACEHOLDER.getTemplateId()));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE)
				.hasChildren(ERROR_MESSAGE)
				.childMinimum(ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER)
				.childMaximum(OTHER_ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER);

		assertThat("There's an error", validationErrors, hasSize(1));
		assertThat("message applied is other error message", validationErrors.get(0),
				validationErrorTextMatches(OTHER_ERROR_MESSAGE));
	}

	@Test
	public void testMaxFindMultipleTemplateIdsFailure() {
		Node meepNode = new Node(PARENT);
		meepNode.addChildNodes(
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.DEFAULT.getTemplateId()));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.childMaximum("too many children", 2, TemplateId.PLACEHOLDER, TemplateId.DEFAULT);

		assertThat("There's an error", validationErrors, hasSize(1));
		assertThat("message applied is other error message", validationErrors.get(0),
				validationErrorTextMatches("too many children"));
	}

	@Test
	public void testMaxFindMultipleTemplateIdsSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.addChildNodes(
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.DEFAULT.getTemplateId()),
				new Node(TemplateId.ACI_AGGREGATE_COUNT.getTemplateId()));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.childMaximum("too many children", 3, TemplateId.PLACEHOLDER, TemplateId.DEFAULT);
		assertThat("There should be no errors.", validationErrors, empty());
	}

	@Test
	public void testValueChildrenChildMinChildMaxFindSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");
		meepNode.addChildNode(new Node(TemplateId.PLACEHOLDER.getTemplateId()));

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.value(ERROR_MESSAGE, VALUE)
				.hasChildren(ERROR_MESSAGE)
				.childMinimum(ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER)
				.childMaximum(OTHER_ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER);
		assertThat("There should be no errors.", validationErrors, empty());
	}

	// compound checking
	@Test
	public void compoundIntValueCheckSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.intValue(ERROR_MESSAGE, VALUE).greaterThan(ERROR_MESSAGE, 122);
		assertThat("There should be no errors.", validationErrors, empty());
	}

	@Test
	public void compoundIntValueCheckFailure() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.intValue(ERROR_MESSAGE, VALUE).greaterThan(ERROR_MESSAGE, 124);
		assertThat("There should be one error.", validationErrors, hasSize(1));
	}

	@Test
	public void compoundIntValueCheckNoContext() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.greaterThan(ERROR_MESSAGE, 124);
		assertThat("There should be no errors.", validationErrors, empty());
	}

	@Test(expected = ClassCastException.class)
	public void compoundIntValueCheckCastException() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");

		Checker checker = Checker.check(meepNode, validationErrors);
		checker.intValue(ERROR_MESSAGE, VALUE).greaterThan(ERROR_MESSAGE, "not an Integer");
		assertThat("There should be no errors.", validationErrors, empty());
	}

	// thorough checking
	@Test
	public void testIntValueChildrenChildMinChildMaxFindFailure() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "Bob");
		meepNode.addChildNodes(
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.PLACEHOLDER.getTemplateId()));

		Checker checker = Checker.thoroughlyCheck(meepNode, validationErrors);
		checker.intValue("int failure", VALUE)
				.hasChildren(ERROR_MESSAGE)
				.childMinimum(ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER)
				.childMaximum("maximum failure", 1, TemplateId.PLACEHOLDER);

		assertThat("There are errors", validationErrors, hasSize(2));
		assertThat("int validation error", validationErrors,
				containsValidationErrorInAnyOrderIgnoringPath("int failure" ,"maximum failure"));
	}

	@Test
	public void testIntValueChildrenChildMinChildMaxFindSuccess() {
		Node meepNode = new Node(PARENT);
		meepNode.putValue(VALUE, "123");
		meepNode.addChildNodes(
				new Node(TemplateId.PLACEHOLDER.getTemplateId()),
				new Node(TemplateId.PLACEHOLDER.getTemplateId()));

		Checker checker = Checker.thoroughlyCheck(meepNode, validationErrors);
		checker.intValue("int failure", VALUE)
				.hasChildren(ERROR_MESSAGE)
				.childMinimum(ERROR_MESSAGE, 1, TemplateId.PLACEHOLDER)
				.childMaximum("maximum failure", 2, TemplateId.PLACEHOLDER);

		assertThat("There should be no errors.", validationErrors, empty());
	}

	@Test
	public void testHasMeasuresSuccess() {
		String measure = "measure";
		String measureId = "measureId";
		String expectedMeasure1 = "asdf";
		String expectedMeasure2 = "jkl;";
		String anotherMeasureId = "DogCow";

		Node section = new Node(PARENT);
		Node measure1 = new Node(measure);
		measure1.putValue(measureId, expectedMeasure1);
		Node measure2 = new Node(measure);
		measure2.putValue(measureId, anotherMeasureId);
		Node measure3 = new Node(measure);
		measure3.putValue(measureId, expectedMeasure2);

		section.addChildNodes(measure1, measure2, measure3);

		Checker checker = Checker.check(section, validationErrors);

		checker.hasMeasures("measure failure", expectedMeasure1, expectedMeasure2);

		assertThat("All the measures should have been found.", validationErrors, empty());
	}

	@Test
	public void testHasMeasuresFailure() {
		String measure = "measure";
		String measureId = "measureId";
		String expectedMeasure = "DogCow";
		String anotherMeausure1 = "asdf";
		String anotherMeausure2 = "jkl;";
		String anotherMeausure3 = "qwerty";
		String validationError = "measure failure";

		Node section = new Node(PARENT);
		Node measure1 = new Node(measure);
		measure1.putValue(measureId, anotherMeausure1);
		Node measure2 = new Node(measure);
		measure2.putValue(measureId, anotherMeausure2);
		Node measure3 = new Node(measure);
		measure3.putValue(measureId, anotherMeausure3);

		section.addChildNodes(measure1, measure2, measure3);

		Checker checker = Checker.check(section, validationErrors);

		checker.hasMeasures(validationError, expectedMeasure);

		assertThat("A measure should not have been found.", validationErrors, hasSize(1));
		assertThat("The validation error string did not match up.", validationErrors.get(0),
				validationErrorTextMatches(validationError));
	}

	@Test
	public void testCheckerHasMeasuresShortCut() {
		List<ValidationError> errors = new ArrayList<>();
		ValidationError err = new ValidationError();
		errors.add(err);
		Node root = new Node();
		Checker checker = Checker.check(root, errors)
				.hasMeasures("Some Message", "MeasureId");

		assertThat("Checker should return one validation error", errors, hasSize(1));

	}

	@Test
	public void testCheckerHasInvalidMeasure() {
		List<ValidationError> errors = new ArrayList<>();

		Node root = new Node();
		Node measure = new Node(root, TemplateId.CLINICAL_DOCUMENT.getTemplateId());
		measure.putValue("NotAmeasure", "0");
		root.addChildNode(measure);
		Checker checker = Checker.check(root, errors)
				.hasMeasures("Some Message", "MeasureId");

		assertThat("Checker should return one validation error", errors, hasSize(1));

	}

	@Test
	public void testHasChildrenWithTemplateIdSuccess() {
		Node iaSectionNode = new Node(TemplateId.IA_SECTION.getTemplateId());
		Node iaMeasureNode = new Node(TemplateId.IA_MEASURE.getTemplateId());
		iaSectionNode.addChildNode(iaMeasureNode);

		Checker checker = Checker.check(iaSectionNode, validationErrors);
		checker.onlyHasChildren(ERROR_MESSAGE, TemplateId.IA_MEASURE);
		assertThat("There should be no errors", validationErrors, empty());
	}

	@Test
	public void testHasChildrenWithTemplateIdFailure() {
		Node iaSectionNode = new Node(TemplateId.IA_SECTION.getTemplateId());
		Node iaMeasureNode = new Node(TemplateId.IA_MEASURE.getTemplateId());
		iaSectionNode.addChildNode(iaMeasureNode);

		Node aggregateCountNode = new Node(TemplateId.ACI_AGGREGATE_COUNT.getTemplateId());
		iaSectionNode.addChildNode(aggregateCountNode);

		Checker checker = Checker.check(iaSectionNode, validationErrors);
		checker.onlyHasChildren(ERROR_MESSAGE, TemplateId.IA_MEASURE);

		assertThat("There should be an error", validationErrors.get(0), validationErrorTextMatches(ERROR_MESSAGE));
	}

	@Test
	public void testValueIn() throws Exception {
		String key = "My Key";
		String value = "My Value";
		Node testNode = makeTestNode(key, value);
		Checker checker = Checker.check(testNode, validationErrors);
		checker.valueIn(ERROR_MESSAGE, key, "No Value" , "Some Value", "My Value");
		assertThat("There should be no errors", validationErrors, empty());
	}

	@Test
	public void testValueInNot() throws Exception {
		String key = "My Key";
		String value = "My Value Not";
		Node testNode = makeTestNode(key, value);
		Checker checker = Checker.check(testNode, validationErrors);
		checker.valueIn(ERROR_MESSAGE, key, "No Value" , "Some Value", "My Value");
		assertThat("There should be 1 error", validationErrors, hasSize(1));
		assertThat("There should be an error", validationErrors.get(0), validationErrorTextMatches(ERROR_MESSAGE));
	}
	@Test
	public void testValueInNull() throws Exception {
		String key = "My Key";
		String value = null;
		Node testNode = makeTestNode(key, value);
		Checker checker = Checker.check(testNode, validationErrors);
		checker.valueIn(ERROR_MESSAGE, key, null);
		assertThat("There should be 1 error", validationErrors, hasSize(1));
		assertThat("There should be an error", validationErrors.get(0), validationErrorTextMatches(ERROR_MESSAGE));
	}
	@Test
	public void testValueInKeyNull() throws Exception {
		String key = "My Key";
		String value = "My Value";
		Node testNode = makeTestNode(key, value);
		Checker checker = Checker.check(testNode, validationErrors);
		checker.valueIn(ERROR_MESSAGE, null, null);
		assertThat("There should be 1 error", validationErrors, hasSize(1));
		assertThat("There should be an error", validationErrors.get(0), validationErrorTextMatches(ERROR_MESSAGE));
	}
	@Test
	public void testValueInNulls() throws Exception {
		String key = "My Key";
		String value = "My Value";
		Node testNode = makeTestNode(key, value);
		Checker checker = Checker.check(testNode, validationErrors);
		checker.valueIn(ERROR_MESSAGE, key, null);
		assertThat("There should be 1 error", validationErrors, hasSize(1));
		assertThat("There should be an error", validationErrors.get(0), validationErrorTextMatches(ERROR_MESSAGE));
	}
	@Test
	public void testValueInShouldShortCut() throws Exception {
		String key = "My Key";
		String value = null;
		Node testNode = makeTestNode(key, value);
		validationErrors.add(new ValidationError(ERROR_MESSAGE));
		Checker checker = Checker.check(testNode, validationErrors);
		checker.valueIn(ERROR_MESSAGE, key, null , "Some Value", "My Value");
		assertThat("There should be 1 error", validationErrors, hasSize(1));
		assertThat("There should be an error", validationErrors.get(0), validationErrorTextMatches(ERROR_MESSAGE));
	}

	private Node makeTestNode(String key, String value) {
		Node testNode = new Node();
		testNode.putValue(key, value);
		return testNode;
	}

}
