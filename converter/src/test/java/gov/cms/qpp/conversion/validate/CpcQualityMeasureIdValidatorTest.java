package gov.cms.qpp.conversion.validate;

import gov.cms.qpp.conversion.model.Node;
import gov.cms.qpp.conversion.model.TemplateId;
import gov.cms.qpp.conversion.model.error.correspondence.DetailsErrorEquals;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

public class CpcQualityMeasureIdValidatorTest {
	private CpcQualityMeasureIdValidator validator;
	private Node testNode;

	@Before
	public void setUp() {
		validator = new CpcQualityMeasureIdValidator();

		testNode = new Node(TemplateId.MEASURE_REFERENCE_RESULTS_CMS_V2);
		testNode.putValue(CpcQualityMeasureIdValidator.MEASURE_ID,"40280381-51f0-825b-0152-22a112d2172a");
	}

	@Test
	public void testPerformanceCountWithNoErrors() {
		addAnyNumberOfChildren(2);
		validator.internalValidateSingleNode(testNode);

		assertWithMessage("Must contain 0 invalid performance rate count errors")
				.that(validator.getDetails()).comparingElementsUsing(DetailsErrorEquals.INSTANCE)
				.doesNotContain(String.format(CpcQualityMeasureIdValidator.INVALID_PERFORMANCE_RATE_COUNT, 2));
	}

	@Test
	public void testPerformanceCountWithIncreasedSizeError() {
		addAnyNumberOfChildren(3);
		validator.internalValidateSingleNode(testNode);

		assertWithMessage("Must contain 2 invalid performance rate count errors")
				.that(validator.getDetails()).comparingElementsUsing(DetailsErrorEquals.INSTANCE)
				.contains(String.format(CpcQualityMeasureIdValidator.INVALID_PERFORMANCE_RATE_COUNT, 2));
	}

	@Test
	public void testPerformanceCountWithDecreasedSizeError() {
		addAnyNumberOfChildren(1);
		validator.internalValidateSingleNode(testNode);

		assertWithMessage("Must contain 2 invalid performance rate count errors")
				.that(validator.getDetails()).comparingElementsUsing(DetailsErrorEquals.INSTANCE)
				.contains(String.format(CpcQualityMeasureIdValidator.INVALID_PERFORMANCE_RATE_COUNT, 2));
	}

	private void addAnyNumberOfChildren(int size) {
		for (int count = 0 ; count < size; count++) {
			Node childNode = new Node(TemplateId.PERFORMANCE_RATE_PROPORTION_MEASURE);
			testNode.addChildNode(childNode);
		}
	}
}
