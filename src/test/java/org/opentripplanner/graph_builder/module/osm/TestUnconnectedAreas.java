package org.opentripplanner.graph_builder.module.osm;

import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestUnconnectedAreas extends TestCase {

    /**
     * The P+R.osm.gz file contains 2 park and ride, one a single way area and the other a
     * multipolygon with a hole. Both are not linked to any street, apart from three roads that
     * crosses the P+R with w/o common nodes.
     * 
     * This test just make sure we correctly link those P+R with the street network by creating
     * virtual nodes at the place where the street intersects the P+R areas. See ticket #1562.
     */
    @Test
    public void testUnconnectedParkAndRide() throws Exception {

        Graph gg = new Graph();

        DataImportIssueStore issueStore = new DataImportIssueStore(true);

        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        File file = new File(getClass().getResource("P+R.osm.pbf").toURI());
        BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(file, false);
        loader.setProvider(provider);
        loader.buildGraph(gg, new HashMap<Class<?>, Object>(), issueStore);

        assertEquals(1, issueStore.getIssues().size());

        int nParkAndRide = 0;
        int nParkAndRideLink = 0;
        for (Vertex v : gg.getVertices()) {
            if (v instanceof ParkAndRideVertex) {
                nParkAndRide++;
            }
        }
        for (Edge e : gg.getEdges()) {
            if (e instanceof ParkAndRideLinkEdge) {
                nParkAndRideLink++;
            }
        }
        assertEquals(2, nParkAndRide);
        assertEquals(10, nParkAndRideLink);
    }
    
    /**
     * This test ensures that if a Park and Ride has a node that is exactly atop a node on a way, the graph
     * builder will not loop forever trying to split it. The hackett-pr.osm.gz file contains a park-and-ride lot in
     * Hackettstown, NJ, which demonstrates this behavior. See discussion in ticket 1605.
     */
    @Test
    public void testCoincidentNodeUnconnectedParkAndRide () throws URISyntaxException {
    	
    	Graph g = new Graph();
    	
        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        File file = new File(getClass().getResource("hackett_pr.osm.pbf").toURI());
        BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(file, false);
        loader.setProvider(provider);
        loader.buildGraph(g, new HashMap<Class<?>, Object>());
    	
        Vertex washTwp = null;
        
        int nParkAndRide = 0;
        int nParkAndRideLink = 0;
        for (Vertex v : g.getVertices()) {
        	if (v instanceof ParkAndRideVertex) {
        		nParkAndRide++;
        		washTwp = v;
        	}
        }
        
        for (Edge e : g.getEdges()) {
            if (e instanceof ParkAndRideLinkEdge) {
                nParkAndRideLink++;
            }
        }
        
        assertEquals(1, nParkAndRide);
        // the P+R should get four connections, since the entrance road is duplicated as well, and crosses twice
        // since there are in and out edges, that brings the total to 8 per P+R.
        // Even though the park and rides get merged, duplicate edges remain from when they were separate.
        // FIXME: we shouldn't have duplicate edges.
        assertEquals(16, nParkAndRideLink);
        
        assertNotNull(washTwp);
        
        List<String> connections = new ArrayList<String>();
        
        for (Edge e : washTwp.getOutgoing()) {
        	if (e instanceof ParkAndRideEdge)
        		continue;
        	
        	assertTrue(e instanceof ParkAndRideLinkEdge);
        	
        	connections.add(e.getToVertex().getLabel());
        }
        
        // symmetry
        for (Edge e : washTwp.getIncoming()) {
        	if (e instanceof ParkAndRideEdge)
        		continue;
        	
        	assertTrue(e instanceof ParkAndRideLinkEdge);
        	assertTrue(connections.contains(e.getFromVertex().getLabel()));
        }
        
        assertTrue(connections.contains("osm:node:3096570222"));
        assertTrue(connections.contains("osm:node:3094264704"));
        assertTrue(connections.contains("osm:node:3094264709"));
        assertTrue(connections.contains("osm:node:3096570227"));
    }
    
    /**
     * Test the situation where a road passes over a node of a park and ride but does not have a node there.
     */
     @Test
     public void testRoadPassingOverNode () throws Exception {
    	 List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr.osm.pbf", 1, 2);
    	 assertTrue(connections.contains("osm:node:-10"));
     }
     
     /**
      * Test the situation where a park and ride passes over the node of a road but does not have a node there.
      * Additionally, the node of the road is duplicated to test this corner case.
      */
     @Test
     public void testAreaPassingOverNode () throws Exception {
    	 List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr.osm.pbf", 1, 2);
    	 assertTrue(connections.contains("osm:node:-10"));
     }
     
     /**
     * Test the situation where a road passes over a node of a park and ride but does not have a node there.
      * Additionally, the node of the ring is duplicated to test this corner case.
      */
     @Test
     public void testRoadPassingOverDuplicatedNode () throws URISyntaxException {
    	 List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr.osm.pbf", 1, 2);
    	 
    	 // depending on what order everything comes out of the spatial index, we will inject one of
    	 // the duplicated nodes into the way. When we get to the other ringsegments, we will just inject
    	 // the node that has already been injected into the way. So either of these cases are valid.
    	 assertTrue(connections.contains("osm:node:-10") || connections.contains("osm:node:-100"));
     }
     
     /**
      * We've written several OSM files that exhibit different situations but should show the same results. Test with this code.
      */
     public List<String> testGeometricGraphWithClasspathFile(String fn, int prCount, int prlCount) throws URISyntaxException {
    	 
     	Graph g = new Graph();
     	
         OpenStreetMapModule loader = new OpenStreetMapModule();
         loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
         File file = new File(getClass().getResource(fn).toURI());
         BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(file, false);
         loader.setProvider(provider);
         loader.buildGraph(g, new HashMap<>());

         Vertex pr = null;
         
         int nParkAndRide = 0;
         int nParkAndRideLink = 0;
         for (Vertex v : g.getVertices()) {
         	if (v instanceof ParkAndRideVertex) {
         		nParkAndRide++;
         		pr = v;
         	}
         }
         
         for (Edge e : g.getEdges()) {
             if (e instanceof ParkAndRideLinkEdge) {
                 nParkAndRideLink++;
             }
         }
         
         assertEquals(prCount, nParkAndRide);
         assertEquals(prlCount, nParkAndRideLink);
         assertNotNull(pr);
         
         // make sure it is connected
        
         List<String> connections = new ArrayList<String>();
         
         for (Edge e : pr.getOutgoing()) {
         	if (e instanceof ParkAndRideEdge)
         		continue;
         	
         	assertTrue(e instanceof ParkAndRideLinkEdge);
         	
         	connections.add(e.getToVertex().getLabel());
         }
         
         // symmetry
         for (Edge e : pr.getIncoming()) {
         	if (e instanceof ParkAndRideEdge)
         		continue;
         	
         	assertTrue(e instanceof ParkAndRideLinkEdge);
         	assertTrue(connections.contains(e.getFromVertex().getLabel()));
         }
         
         return connections;
     }
}
