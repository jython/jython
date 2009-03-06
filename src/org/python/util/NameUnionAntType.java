package org.python.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.BaseResourceCollectionContainer;

/**
 * Unions several resource collections by the name of their contained resources.
 */
public class NameUnionAntType extends BaseResourceCollectionContainer {
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Resource> getCollection() {
        List<ResourceCollection> collections = getResourceCollections();
        // preserve order-encountered using a list; keep track of the items with a set
        Set<String> seenNames = Generic.set();
        List<Resource> union = new ArrayList();
        for (ResourceCollection rc : collections) {
            for (Iterator<Resource> resources = rc.iterator(); resources.hasNext();) {
                Resource r = resources.next();
                if (seenNames.add(r.getName())) {
                    union.add(r);
                }
            }
        }
        return union;
    }
}
