package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.*;
import org.bbop.apollo.gwt.shared.geneProduct.Reference;
import org.bbop.apollo.gwt.shared.geneProduct.WithOrFrom;
import org.bbop.apollo.gwt.shared.geneProduct.GeneProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class GeneProductConverter {

  public static GeneProduct convertFromJson(JSONObject object) {
    GeneProduct geneProduct = new GeneProduct();

//                    "geneRelationship":"RO:0002326", "goTerm":"GO:0031084", "references":"[\"ref:12312\"]", "gene":
//                    "1743ae6c-9a37-4a41-9b54-345065726d5f", "negate":false, "evidenceCode":"ECO:0000205", "withOrFrom":
//                    "[\"adf:12312\"]"
    geneProduct.setId(Math.round(object.get("id").isNumber().doubleValue()));
    geneProduct.setFeature(object.get("gene").isString().stringValue());
    geneProduct.setProductName(object.get("productName").isString().stringValue());
    geneProduct.setAlternate(object.get("alternate").isBoolean().booleanValue());
//    geneProduct.setGoTerm(object.get("goTerm").isString().stringValue());
    if(object.containsKey("evidenceCodeLabel")){
      geneProduct.setEvidenceCodeLabel(object.get("evidenceCodeLabel").isString().stringValue());
    }
    geneProduct.setEvidenceCode(object.get("evidenceCode").isString().stringValue());
    geneProduct.setReference(new Reference(object.get("reference").isString().stringValue()));


    List<WithOrFrom> withOrFromList = new ArrayList<>();
    if (object.get("withOrFrom").isString() != null) {
      String withOrFromString = object.get("withOrFrom").isString().stringValue();
      JSONArray withOrFromArray = JSONParser.parseStrict(withOrFromString).isArray();
      for (int i = 0; i < withOrFromArray.size(); i++) {
        WithOrFrom withOrFrom = new WithOrFrom(withOrFromArray.get(i).isString().stringValue());
        withOrFromList.add(withOrFrom);
      }
    }
    geneProduct.setWithOrFromList(withOrFromList);

    return geneProduct;
  }

  public static JSONObject convertToJson(GeneProduct geneProduct) {
    JSONObject object = new JSONObject();

    // TODO: an NPE in here, somehwere
    if (geneProduct.getId() != null) {
      object.put("id", new JSONNumber(geneProduct.getId()));
    }
    object.put("gene", new JSONString(geneProduct.getFeature()));
    object.put("productName", new JSONString(geneProduct.getProductName()));
    object.put("alternate", JSONBoolean.getInstance(geneProduct.isAlternate()));
    object.put("evidenceCode", new JSONString(geneProduct.getEvidenceCode()));
    object.put("evidenceCodeLabel", new JSONString(geneProduct.getEvidenceCodeLabel()));
    object.put("reference", new JSONString(geneProduct.getReference().getReferenceString()));

    // TODO: finish this
    JSONArray withArray = new JSONArray();

    for (WithOrFrom withOrFrom : geneProduct.getWithOrFromList()) {
      withArray.set(withArray.size(), new JSONString(withOrFrom.getDisplay()));
    }

    object.put("withOrFrom", withArray);

    return object;
  }
}
