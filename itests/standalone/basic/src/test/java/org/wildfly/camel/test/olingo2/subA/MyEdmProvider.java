/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.olingo2.subA;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.odata2.api.edm.EdmConcurrencyMode;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.EdmTargetPath;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationEnd;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.AssociationSetEnd;
import org.apache.olingo.odata2.api.edm.provider.ComplexProperty;
import org.apache.olingo.odata2.api.edm.provider.ComplexType;
import org.apache.olingo.odata2.api.edm.provider.CustomizableFeedMappings;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntityContainerInfo;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.FunctionImport;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.ReturnType;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.exception.ODataException;

public class MyEdmProvider extends EdmProvider {

    static final String ENTITY_SET_NAME_MANUFACTURERS = "Manufacturers";
    static final String ENTITY_SET_NAME_CARS = "Cars";
    static final String ENTITY_NAME_MANUFACTURER = "Manufacturer";
    static final String ENTITY_NAME_CAR = "Car";

    private static final String NAMESPACE = "org.apache.olingo.odata2.ODataCars";

    private static final FullQualifiedName ENTITY_TYPE_1_1 = new FullQualifiedName(NAMESPACE, ENTITY_NAME_CAR);
    private static final FullQualifiedName ENTITY_TYPE_1_2 = new FullQualifiedName(NAMESPACE, ENTITY_NAME_MANUFACTURER);

    private static final FullQualifiedName COMPLEX_TYPE = new FullQualifiedName(NAMESPACE, "Address");

    private static final FullQualifiedName ASSOCIATION_CAR_MANUFACTURER = new FullQualifiedName(NAMESPACE, "Car_Manufacturer_Manufacturer_Cars");

    private static final String ROLE_1_1 = "Car_Manufacturer";
    private static final String ROLE_1_2 = "Manufacturer_Cars";

    private static final String ENTITY_CONTAINER = "ODataCarsEntityContainer";

    private static final String ASSOCIATION_SET = "Cars_Manufacturers";

    private static final String FUNCTION_IMPORT = "NumberOfCars";

    @Override
    public List<Schema> getSchemas() throws ODataException {
        List<Schema> schemas = new ArrayList<>();

        Schema schema = new Schema();
        schema.setNamespace(NAMESPACE);

        List<EntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(ENTITY_TYPE_1_1));
        entityTypes.add(getEntityType(ENTITY_TYPE_1_2));
        schema.setEntityTypes(entityTypes);

        List<ComplexType> complexTypes = new ArrayList<>();
        complexTypes.add(getComplexType(COMPLEX_TYPE));
        schema.setComplexTypes(complexTypes);

        List<Association> associations = new ArrayList<>();
        associations.add(getAssociation(ASSOCIATION_CAR_MANUFACTURER));
        schema.setAssociations(associations);

        List<EntityContainer> entityContainers = new ArrayList<>();
        EntityContainer entityContainer = new EntityContainer();
        entityContainer.setName(ENTITY_CONTAINER).setDefaultEntityContainer(true);

        List<EntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(ENTITY_CONTAINER, ENTITY_SET_NAME_CARS));
        entitySets.add(getEntitySet(ENTITY_CONTAINER, ENTITY_SET_NAME_MANUFACTURERS));
        entityContainer.setEntitySets(entitySets);

        List<AssociationSet> associationSets = new ArrayList<>();
        associationSets.add(getAssociationSet(ENTITY_CONTAINER, ASSOCIATION_CAR_MANUFACTURER, ENTITY_SET_NAME_MANUFACTURERS, ROLE_1_2));
        entityContainer.setAssociationSets(associationSets);

        List<FunctionImport> functionImports = new ArrayList<>();
        functionImports.add(getFunctionImport(ENTITY_CONTAINER, FUNCTION_IMPORT));
        entityContainer.setFunctionImports(functionImports);

        entityContainers.add(entityContainer);
        schema.setEntityContainers(entityContainers);

        schemas.add(schema);

        return schemas;
    }

    @Override
    public EntityType getEntityType(FullQualifiedName edmFQName) throws ODataException {
        if (NAMESPACE.equals(edmFQName.getNamespace())) {

            if (ENTITY_TYPE_1_1.getName().equals(edmFQName.getName())) {

                //Properties
                List<Property> properties = new ArrayList<>();
                properties.add(new SimpleProperty().setName("Id").setType(EdmSimpleTypeKind.Int32).setFacets(new Facets().setNullable(false)));
                properties.add(new SimpleProperty().setName("Model").setType(EdmSimpleTypeKind.String).setFacets(new Facets().setNullable(false).setMaxLength(100).setDefaultValue("Hugo"))
                        .setCustomizableFeedMappings(new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_TITLE)));
                properties.add(new SimpleProperty().setName("ManufacturerId").setType(EdmSimpleTypeKind.Int32));
                properties.add(new SimpleProperty().setName("Price").setType(EdmSimpleTypeKind.Decimal));
                properties.add(new SimpleProperty().setName("Currency").setType(EdmSimpleTypeKind.String).setFacets(new Facets().setMaxLength(3)));
                properties.add(new SimpleProperty().setName("ModelYear").setType(EdmSimpleTypeKind.String).setFacets(new Facets().setMaxLength(4)));
                properties.add(new SimpleProperty().setName("Updated").setType(EdmSimpleTypeKind.DateTime)
                        .setFacets(new Facets().setNullable(false).setConcurrencyMode(EdmConcurrencyMode.Fixed))
                        .setCustomizableFeedMappings(new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_UPDATED)));
                properties.add(new SimpleProperty().setName("ImagePath").setType(EdmSimpleTypeKind.String));

                //Navigation Properties
                List<NavigationProperty> navigationProperties = new ArrayList<>();
                navigationProperties.add(new NavigationProperty().setName("Manufacturer")
                        .setRelationship(ASSOCIATION_CAR_MANUFACTURER).setFromRole(ROLE_1_1).setToRole(ROLE_1_2));

                //Key
                List<PropertyRef> keyProperties = new ArrayList<>();
                keyProperties.add(new PropertyRef().setName("Id"));
                Key key = new Key().setKeys(keyProperties);

                return new EntityType().setName(ENTITY_TYPE_1_1.getName())
                        .setProperties(properties)
                        .setKey(key)
                        .setNavigationProperties(navigationProperties);

            } else if (ENTITY_TYPE_1_2.getName().equals(edmFQName.getName())) {

                //Properties
                List<Property> properties = new ArrayList<>();
                properties.add(new SimpleProperty().setName("Id").setType(EdmSimpleTypeKind.Int32).setFacets(new Facets().setNullable(false)));
                properties.add(new SimpleProperty().setName("Name").setType(EdmSimpleTypeKind.String).setFacets(new Facets().setNullable(false).setMaxLength(100))
                        .setCustomizableFeedMappings(new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_TITLE)));
                properties.add(new ComplexProperty().setName("Address").setType(new FullQualifiedName(NAMESPACE, "Address")));
                properties.add(new SimpleProperty().setName("Updated").setType(EdmSimpleTypeKind.DateTime)
                        .setFacets(new Facets().setNullable(false).setConcurrencyMode(EdmConcurrencyMode.Fixed))
                        .setCustomizableFeedMappings(new CustomizableFeedMappings().setFcTargetPath(EdmTargetPath.SYNDICATION_UPDATED)));

                //Navigation Properties
                List<NavigationProperty> navigationProperties = new ArrayList<>();
                navigationProperties.add(new NavigationProperty().setName("Cars")
                        .setRelationship(ASSOCIATION_CAR_MANUFACTURER).setFromRole(ROLE_1_2).setToRole(ROLE_1_1));

                //Key
                List<PropertyRef> keyProperties = new ArrayList<>();
                keyProperties.add(new PropertyRef().setName("Id"));
                Key key = new Key().setKeys(keyProperties);

                return new EntityType().setName(ENTITY_TYPE_1_2.getName())
                        .setProperties(properties)
                        .setKey(key)
                        .setNavigationProperties(navigationProperties);

            }
        }

        return null;
    }

    @Override
    public ComplexType getComplexType(FullQualifiedName edmFQName) throws ODataException {
        if (NAMESPACE.equals(edmFQName.getNamespace())) {
            if (COMPLEX_TYPE.getName().equals(edmFQName.getName())) {
                List<Property> properties = new ArrayList<>();
                properties.add(new SimpleProperty().setName("Street").setType(EdmSimpleTypeKind.String));
                properties.add(new SimpleProperty().setName("City").setType(EdmSimpleTypeKind.String));
                properties.add(new SimpleProperty().setName("ZipCode").setType(EdmSimpleTypeKind.String));
                properties.add(new SimpleProperty().setName("Country").setType(EdmSimpleTypeKind.String));
                return new ComplexType().setName(COMPLEX_TYPE.getName()).setProperties(properties);
            }
        }

        return null;
    }

    @Override
    public Association getAssociation(FullQualifiedName edmFQName) throws ODataException {
        if (NAMESPACE.equals(edmFQName.getNamespace())) {
            if (ASSOCIATION_CAR_MANUFACTURER.getName().equals(edmFQName.getName())) {
                return new Association().setName(ASSOCIATION_CAR_MANUFACTURER.getName())
                        .setEnd1(new AssociationEnd().setType(ENTITY_TYPE_1_1).setRole(ROLE_1_1).setMultiplicity(EdmMultiplicity.MANY))
                        .setEnd2(new AssociationEnd().setType(ENTITY_TYPE_1_2).setRole(ROLE_1_2).setMultiplicity(EdmMultiplicity.ONE));
            }
        }
        return null;
    }

    @Override
    public EntitySet getEntitySet(String entityContainer, String name) throws ODataException {
        if (ENTITY_CONTAINER.equals(entityContainer)) {
            if (ENTITY_SET_NAME_CARS.equals(name)) {
                return new EntitySet().setName(name).setEntityType(ENTITY_TYPE_1_1);
            } else if (ENTITY_SET_NAME_MANUFACTURERS.equals(name)) {
                return new EntitySet().setName(name).setEntityType(ENTITY_TYPE_1_2);
            }
        }
        return null;
    }

    @Override
    public AssociationSet getAssociationSet(String entityContainer, FullQualifiedName association, String sourceEntitySetName, String sourceEntitySetRole) throws ODataException {
        if (ENTITY_CONTAINER.equals(entityContainer)) {
            if (ASSOCIATION_CAR_MANUFACTURER.equals(association)) {
                return new AssociationSet().setName(ASSOCIATION_SET)
                        .setAssociation(ASSOCIATION_CAR_MANUFACTURER)
                        .setEnd1(new AssociationSetEnd().setRole(ROLE_1_2).setEntitySet(ENTITY_SET_NAME_MANUFACTURERS))
                        .setEnd2(new AssociationSetEnd().setRole(ROLE_1_1).setEntitySet(ENTITY_SET_NAME_CARS));
            }
        }
        return null;
    }

    @Override
    public FunctionImport getFunctionImport(String entityContainer, String name) throws ODataException {
        if (ENTITY_CONTAINER.equals(entityContainer)) {
            if (FUNCTION_IMPORT.equals(name)) {
                return new FunctionImport().setName(name)
                        .setReturnType(new ReturnType().setTypeName(ENTITY_TYPE_1_1).setMultiplicity(EdmMultiplicity.MANY))
                        .setHttpMethod("GET");
            }
        }
        return null;
    }

    @Override
    public EntityContainerInfo getEntityContainerInfo(String name) throws ODataException {
        if (name == null || "ODataCarsEntityContainer".equals(name)) {
            return new EntityContainerInfo().setName("ODataCarsEntityContainer").setDefaultEntityContainer(true);
        }

        return null;
    }
}
