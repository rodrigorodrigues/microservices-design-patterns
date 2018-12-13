package com.learning;

import com.learning.springboot.Java8SpringBootApplication;
import com.learning.wsdl.client.Asset;
import com.learning.wsdl.client.AssetStatus;
import com.learning.wsdl.client.CreateOrUpdateAsset;
import com.learning.wsdl.client.ObjectFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.xml.transform.StringResult;

import javax.xml.bind.JAXBElement;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Java8SpringBootApplication.class)
public class Java8SpringBootApplicationTests {

	@Test
	public void testWsdlClient() {
		ObjectFactory objectFactory = new ObjectFactory();
		CreateOrUpdateAsset createOrUpdateAsset = objectFactory.createCreateOrUpdateAsset();
		Asset asset = objectFactory.createAsset();
		AssetStatus assetStatus = objectFactory.createAssetStatus();
		assetStatus.setAssetStatusId(111);
		asset.setAssetStatus(assetStatus);
		createOrUpdateAsset.setArg0(asset);

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("com.learning.wsdl.client");

		JAXBElement<CreateOrUpdateAsset> jaxbElement = objectFactory.createCreateOrUpdateAsset(createOrUpdateAsset);

		StringResult result = new StringResult();
		marshaller.marshal(jaxbElement, result);

		assertThat(result.toString()).isNotEmpty();
	}

}
