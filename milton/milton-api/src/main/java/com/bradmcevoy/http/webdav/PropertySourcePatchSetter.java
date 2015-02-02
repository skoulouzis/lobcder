package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.values.ValueAndType;
import com.bradmcevoy.http.values.ValueWriters;
import com.bradmcevoy.http.webdav.PropFindResponse.NameAndError;
import com.bradmcevoy.http.webdav.PropPatchRequestParser.ParseResult;
import com.bradmcevoy.property.PropertySource;
import com.bradmcevoy.property.PropertySource.PropertyMetaData;
import com.bradmcevoy.property.PropertySource.PropertySetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class PropertySourcePatchSetter implements PropPatchSetter {

	private static final Logger log = LoggerFactory.getLogger(PropertySourcePatchSetter.class);
	private final List<PropertySource> propertySources;
	private final ValueWriters valueWriters;

	public PropertySourcePatchSetter(List<PropertySource> propertySources, ValueWriters valueWriters) {
		this.propertySources = propertySources;
		this.valueWriters = valueWriters;
	}

	public PropertySourcePatchSetter(List<PropertySource> propertySources) {
		this.propertySources = propertySources;
		this.valueWriters = new ValueWriters();
	}

	/**
	 * This returns true for all resources, but it actually depends on the
	 * configured property sources.
	 *
	 * If no property sources support a given resource, a proppatch attempt
	 * will return 404's for all properties
	 *
	 * @param r
	 * @return
	 */
	@Override
	public boolean supports(Resource r) {
		return true;
	}

	@Override
	public PropFindResponse setProperties(String href, ParseResult parseResult, Resource r) {
		log.trace("setProperties: resource type: {}", r.getClass());
		Map<QName, ValueAndType> knownProps = new HashMap<QName, ValueAndType>();

		Map<Status, List<NameAndError>> errorProps = new EnumMap<Status, List<NameAndError>>(Status.class);
		for (Entry<QName, String> entry : parseResult.getFieldsToSet().entrySet()) {
			QName name = entry.getKey();
			boolean found = false;
			for (PropertySource source : propertySources) {
				PropertyMetaData meta = source.getPropertyMetaData(entry.getKey(), r);
				if (meta != null && !meta.isUnknown()) {
					found = true;
					if (meta.isWritable()) {
						Object val = parse(name, entry.getValue(), meta.getValueType());
						try {
							log.trace("setProperties: name: {} source: {}", name, source.getClass());
							source.setProperty(name, val, r);
							knownProps.put(name, new ValueAndType(null, meta.getValueType()));
							break;
						} catch (NotAuthorizedException e) {
							log.warn("setProperties: NotAuthorised to write property: {}", name, e);
							addErrorProp(errorProps, Response.Status.SC_UNAUTHORIZED, name, "Not authorised");
							break;
						} catch (PropertySetException ex) {
							log.warn("setProperties: PropertySetException when writing property {}", name, ex);
							addErrorProp(errorProps, ex.getStatus(), name, ex.getErrorNotes());
							break;
						}
					} else {
						log.warn("property is not writable in source: " + source.getClass());
						addErrorProp(errorProps, Response.Status.SC_FORBIDDEN, name, "Property is read only");
						break;
					}
				} else {
					//log.debug( "not found in: " + source.getClass().getCanonicalName() );
				}
			}
			if (!found) {
				log.warn("property not found: " + entry.getKey());
				addErrorProp(errorProps, Status.SC_NOT_FOUND, entry.getKey(), "Unknown property");
			}
		}
		if (parseResult.getFieldsToRemove() != null) {
			for (QName name : parseResult.getFieldsToRemove()) {
				boolean found = false;
				for (PropertySource source : propertySources) {
					PropertyMetaData meta = source.getPropertyMetaData(name, r);
					if (meta != null && !meta.isUnknown()) {
						found = true;
						if (meta.isWritable()) {
							try {
								log.trace("clearProperty");
								source.clearProperty(name, r);
								knownProps.put(name, new ValueAndType(null, meta.getValueType()));
								break;
							} catch (NotAuthorizedException e) {
								addErrorProp(errorProps, Response.Status.SC_UNAUTHORIZED, name, "Not authorised");
								break;
							} catch (PropertySetException ex) {
								addErrorProp(errorProps, ex.getStatus(), name, ex.getErrorNotes());
								break;
							}
						} else {
							log.warn("property is not writable in source: " + source.getClass());
							addErrorProp(errorProps, Response.Status.SC_FORBIDDEN, name, "Property is read only");
							break;
						}
					} else {
						//log.debug( "not found in: " + source.getClass().getCanonicalName() );
					}
				}
				if (!found) {
					log.warn("property not found to remove: " + name);
					addErrorProp(errorProps, Status.SC_NOT_FOUND, name, "Unknown property");
				}
			}
		}
		if (log.isDebugEnabled()) {
			if (errorProps.size() > 0) {
				log.debug("errorProps: " + errorProps.size() + " listing property sources:");
				for (PropertySource s : propertySources) {
					log.debug("  source: " + s.getClass().getCanonicalName());
				}
			}
		}
		if( r instanceof CommitableResource) {
			log.trace("resource is commitable, call doCommit");
			CommitableResource cr = (CommitableResource) r;
			cr.doCommit(knownProps, errorProps);
		} else {
			log.trace("resource is not commitable");
		}
		PropFindResponse resp = new PropFindResponse(href, knownProps, errorProps);
		return resp;
	}

	private void addErrorProp(Map<Status, List<NameAndError>> errorProps, Status stat, QName name, String err) {
		List<NameAndError> list = errorProps.get(stat);
		if (list == null) {
			list = new ArrayList<NameAndError>();
			errorProps.put(stat, list);
		}
		NameAndError ne = new NameAndError(name, err);
		list.add(ne);

	}

	private Object parse(QName key, String value, Class valueType) {
		return valueWriters.parse(key, valueType, value);
	}

	public interface CommitableResource extends Resource {

		void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps);
	}
}
