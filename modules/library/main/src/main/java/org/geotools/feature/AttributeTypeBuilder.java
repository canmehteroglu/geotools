/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Classes;
import org.geotools.util.SimpleInternationalString;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

/**
 * Builder for attribute types and descriptors.
 *
 * <p>Building an attribute type:
 *
 * <pre>
 * <code>
 *  //create the builder
 *  AttributeTypeBuilder builder = new AttributeTypeBuilder();
 *
 *  //set type information
 *  builder.setName( "intType" ):
 *  builder.setBinding( Integer.class );
 *  builder.setNillable( false );
 *
 *  //build the type
 *  AttributeType type = builder.buildType();
 * </code>
 * </pre>
 *
 * <p>Building an attribute descriptor:
 *
 * <pre>
 * <code>
 *  //create the builder
 *  AttributeTypeBuilder builder = new AttributeTypeBuilder();
 *
 *  //set type information
 *  builder.setName( "intType" ):
 *  builder.setBinding( Integer.class );
 *  builder.setNillable( false );
 *
 *  //set descriptor information
 *  builder.setMinOccurs(0);
 *  builder.setMaxOccurs(1);
 *  builder.setNillable(true);
 *
 *  //build the descriptor
 *  AttributeDescriptor descriptor = builder.buildDescriptor("intProperty");
 * </code>
 * </pre>
 *
 * <p>This class maintains state and is not thread safe.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class AttributeTypeBuilder {

    /** factory */
    protected FeatureTypeFactory factory;

    // AttributeType
    //
    /** Local name used to name a descriptor; or combined with namespaceURI to name a type. */
    protected String name;

    /** Separator used to combine namespaceURI and name. */
    private String separator = ":";

    /** namespace used to distingish between otherwise identical type names. */
    protected String namespaceURI;
    /** abstract flag */
    protected boolean isAbstract = false;
    /** restrictions */
    protected List<Filter> restrictions;
    /** string description */
    protected InternationalString description;
    /** identifiable flag */
    protected boolean isIdentifiable = false;
    /** bound java class */
    protected Class<?> binding;
    /** super type */
    protected AttributeType superType;
    /** default value */
    protected Object defaultValue;

    protected boolean isDefaultValueSet = false;

    // GeometryType
    //
    protected CoordinateReferenceSystem crs;
    protected boolean isCrsSet = false;

    // AttributeDescriptor
    //
    /**
     * Minimum number of occurrences allowed. See minOccurs() function for the default value based
     * on nillable if not explicitly set.
     */
    protected Integer minOccurs = null;

    /**
     * Maximum number of occurrences allowed. See maxOccurs() function for the default value (of 1).
     */
    protected Integer maxOccurs = null;

    /**
     * True if value is allowed to be null.
     *
     * <p>Depending on this value minOccurs, maxOccurs and defaultValue() will return different
     * results.
     *
     * <p>The default value is <code>true</code>.
     */
    protected boolean isNillable = true;

    /**
     * If this value is set an additional restriction will be added based on the length function.
     */
    protected Integer length = null;

    /** User data for the attribute. */
    protected Map<Object, Object> userData = null;

    /** filter factory */
    protected FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    /** The list of valid values for attributes described by this type (enumeration). */
    private List<?> options;

    /** Constructs the builder. */
    public AttributeTypeBuilder() {
        this(CommonFactoryFinder.getFeatureTypeFactory(null));
        init();
    }

    /** Constructs the builder specifying the factory used to build attribute types. */
    public AttributeTypeBuilder(FeatureTypeFactory factory) {
        this.factory = factory;
        init();
    }

    /** Resets all internal state. */
    protected void init() {
        resetTypeState();
        resetDescriptorState();
    }

    /**
     * Resets all builder state used to build the attribute type.
     *
     * <p>This method is called automatically after {@link #buildType()} and {@link
     * #buildGeometryType()}.
     */
    protected void resetTypeState() {
        name = null;
        namespaceURI = null;
        isAbstract = false;
        restrictions = null;
        description = null;
        isIdentifiable = false;
        binding = null;
        superType = null;
        crs = null;
        length = null;
        isCrsSet = false;
    }

    protected void resetDescriptorState() {
        resetTypeState();
        minOccurs = null;
        maxOccurs = null;
        isNillable = true;
        userData = new HashMap<>();
        defaultValue = null;
        isDefaultValueSet = false;
        length = null;
        options = null;
    }

    public AttributeTypeBuilder setFactory(FeatureTypeFactory factory) {
        this.factory = factory;
        return this;
    }

    /** Initializes builder state from another attribute type. */
    public AttributeTypeBuilder init(AttributeType type) {
        name = type.getName().getLocalPart();
        separator = type.getName().getSeparator();
        namespaceURI = type.getName().getNamespaceURI();
        isAbstract = type.isAbstract();

        if (type.getRestrictions() != null) {
            restrictions().addAll(type.getRestrictions());
        }

        description = type.getDescription();
        isIdentifiable = type.isIdentified();
        binding = type.getBinding();
        superType = type.getSuper();

        if (type instanceof GeometryType) {
            crs = ((GeometryType) type).getCoordinateReferenceSystem();
        }
        return this;
    }

    /** Initializes builder state from another attribute descriptor. */
    public void init(AttributeDescriptor descriptor) {
        init(descriptor.getType());
        minOccurs = descriptor.getMinOccurs();
        maxOccurs = descriptor.getMaxOccurs();
        isNillable = descriptor.isNillable();
        userData = descriptor.getUserData();
    }

    // Type methods
    //

    public void setBinding(Class<?> binding) {
        this.binding = binding;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
        isCrsSet = true;
    }

    public boolean isCRSSet() {
        return isCrsSet;
    }

    public void setDescription(String description) {
        this.description = description != null ? new SimpleInternationalString(description) : null;
    }

    public void setDescription(InternationalString description) {
        this.description = description;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public void setIdentifiable(boolean isIdentifiable) {
        this.isIdentifiable = isIdentifiable;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void addRestriction(Filter restriction) {
        restrictions().add(restriction);
    }

    public void addUserData(Object key, Object value) {
        userData.put(key, value);
    }

    // Descriptor methods
    //

    public void setNillable(boolean isNillable) {
        this.isNillable = isNillable;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        isDefaultValueSet = true;
    }

    public AttributeTypeBuilder binding(Class<?> binding) {
        setBinding(binding);
        return this;
    }

    public AttributeTypeBuilder name(String name) {
        setName(name);
        return this;
    }

    public AttributeTypeBuilder namespaceURI(String namespaceURI) {
        setNamespaceURI(namespaceURI);
        return this;
    }

    public AttributeTypeBuilder crs(CoordinateReferenceSystem crs) {
        setCRS(crs);
        return this;
    }

    public AttributeTypeBuilder description(String description) {
        setDescription(description);
        return this;
    }

    public AttributeTypeBuilder abstrct(boolean isAbstract) {
        setAbstract(isAbstract);
        return this;
    }

    public AttributeTypeBuilder identifiable(boolean isIdentifiable) {
        setIdentifiable(isIdentifiable);
        return this;
    }

    public AttributeTypeBuilder length(int length) {
        setLength(length);
        return this;
    }

    /**
     * Sets a list of possible valid values for the attribute type being built, and returns a
     * reference to the builder itself
     */
    public AttributeTypeBuilder options(List<?> options) {
        setOptions(options);
        return this;
    }

    /** Sets a list of possible valid values for the attribute type being built */
    public void setOptions(List<?> options) {
        this.options = options;
    }

    public AttributeTypeBuilder restriction(Filter restriction) {
        addRestriction(restriction);
        return this;
    }

    // Descriptor methods
    //

    public AttributeTypeBuilder nillable(boolean isNillable) {
        setNillable(isNillable);
        return this;
    }

    public AttributeTypeBuilder maxOccurs(int maxOccurs) {
        setMaxOccurs(maxOccurs);
        return this;
    }

    public AttributeTypeBuilder minOccurs(int minOccurs) {
        setMinOccurs(minOccurs);
        return this;
    }

    public AttributeTypeBuilder defaultValue(Object defaultValue) {
        setDefaultValue(defaultValue);
        return this;
    }

    public AttributeTypeBuilder userData(Object key, Object value) {
        addUserData(key, value);
        return this;
    }

    public AttributeTypeBuilder superType(AttributeType superType) {
        this.superType = superType;
        return this;
    }

    // construction methods
    //

    /**
     * Builds the attribute type.
     *
     * <p>This method resets all state after the attribute is built.
     */
    public AttributeType buildType() {
        if (length != null) {
            Filter lengthRestriction = lengthRestriction(length);
            restrictions().add(lengthRestriction);
        }

        if (options != null && !options.isEmpty()) {
            Filter optionsRestriction = FeatureTypes.createFieldOptions(options);
            restrictions().add(optionsRestriction);
        }

        AttributeType type =
                factory.createAttributeType(
                        name(),
                        binding,
                        isIdentifiable,
                        isAbstract,
                        restrictions(),
                        superType,
                        description());
        resetTypeState();

        return type;
    }

    /**
     * Builds the geometry attribute type.
     *
     * <p>This method resets all state after the attribute is built.
     */
    public GeometryType buildGeometryType() {
        GeometryType type =
                factory.createGeometryType(
                        name(),
                        binding,
                        crs,
                        isIdentifiable,
                        isAbstract,
                        restrictions(),
                        superType,
                        description());

        resetTypeState();

        return type;
    }

    private Name name() {
        if (name == null) {
            name = Classes.getShortName(binding);
        }
        return separator == null
                ? new NameImpl(namespaceURI, name)
                : new NameImpl(namespaceURI, separator, name);
    }

    private InternationalString description() {
        return description;
    }

    /**
     * Builds an attribute descriptor first building an attribute type from internal state.
     *
     * <p>If {@link #crs} has been set via {@link #setCRS(CoordinateReferenceSystem)}, or {@link
     * #binding} is of Geometry. The internal attribute type will be built via {@link
     * #buildGeometryType()}, and {@link #buildDescriptor(String, GeometryType)} will be called.
     *
     * <p>Otherwise it will be built via {@link #buildType()}, and {@link #buildDescriptor(String,
     * AttributeType)} will be called.
     *
     * @param name The name of the descriptor.
     * @see #buildDescriptor(String, AttributeType)
     * @throws IllegalStateException If no binding is set
     */
    public AttributeDescriptor buildDescriptor(String name) {
        if (binding == null) {
            throw new IllegalStateException("No binding has been provided for this attribute");
        }
        if (crs != null || Geometry.class.isAssignableFrom(binding)) {
            return buildDescriptor(name, buildGeometryType());
        } else {
            return buildDescriptor(name, buildType());
        }
    }

    /**
     * Builds an attribute descriptor specifying its attribute type.
     *
     * <p>Internal state is reset after the descriptor is built.
     *
     * @param name The name of the descriptor.
     * @param type The type referenced by the descriptor.
     */
    public AttributeDescriptor buildDescriptor(String name, AttributeType type) {
        return buildDescriptor(new NameImpl(name), type);
    }

    /**
     * Builds a geometry descriptor specifying its attribute type.
     *
     * <p>Internal state is reset after the descriptor is built.
     *
     * @param name The name of the descriptor.
     * @param type The geometry type referenced by the descriptor.
     */
    public GeometryDescriptor buildDescriptor(String name, GeometryType type) {
        return buildDescriptor(new NameImpl(name), type);
    }

    public AttributeDescriptor buildDescriptor(Name name, AttributeType type) {

        // build the descriptor
        AttributeDescriptor descriptor =
                factory.createAttributeDescriptor(
                        type, name, minOccurs(), maxOccurs(), isNillable, defaultValue());

        // set the user data
        descriptor.getUserData().putAll(userData);
        resetDescriptorState();
        return descriptor;
    }

    public GeometryDescriptor buildDescriptor(Name name, GeometryType type) {
        GeometryDescriptor descriptor =
                factory.createGeometryDescriptor(
                        type, name, minOccurs(), maxOccurs(), isNillable, defaultValue());

        // set the user data
        descriptor.getUserData().putAll(userData);
        resetDescriptorState();
        return descriptor;
    }

    /**
     * This is not actually right but we do it for backwards compatibility.
     *
     * @return minOccurs if set or a default based on isNillable.
     */
    private int minOccurs() {
        if (minOccurs == null) {
            return isNillable ? 0 : 1;
        }
        return minOccurs;
    }
    /**
     * This is not actually right but we do it for backwards compatibility.
     *
     * @return minOccurs if set or a default based on isNillable.
     */
    private int maxOccurs() {
        if (maxOccurs == null) {
            return 1;
        }
        return maxOccurs;
    }

    private Object defaultValue() {
        if (defaultValue == null && !isNillable && binding != null) {
            defaultValue = DataUtilities.defaultValue(binding);
        }
        return defaultValue;
    }

    // internal / subclass api
    //

    protected List<Filter> restrictions() {
        if (restrictions == null) {
            restrictions = new ArrayList<>();
        }

        return restrictions;
    }

    /** Helper method to create a "length" filter. */
    protected Filter lengthRestriction(int length) {
        return FeatureTypes.createLengthRestriction(length);
    }
}
