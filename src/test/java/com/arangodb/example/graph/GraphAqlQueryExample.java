/*
 * Copyright (C) 2015 ArangoDB GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arangodb.example.graph;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.Direction;
import com.arangodb.EdgeCursor;
import com.arangodb.VertexCursor;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.ShortestPathEntity;
import com.arangodb.entity.marker.VertexEntity;
import com.arangodb.util.GraphEdgesOptions;
import com.arangodb.util.ShortestPathOptions;

/**
 * AQL example with new cursor implementation
 * 
 * @author a-brandt
 *
 */
public class GraphAqlQueryExample extends BaseExample {

	private static final String DATABASE_NAME = "GraphAqlQueryExample";

	private static final String GRAPH_NAME = "example_graph1";
	private static final String EDGE_COLLECTION_NAME = "edgeColl1";
	private static final String VERTEXT_COLLECTION_NAME = "vertexColl1";

	public ArangoDriver arangoDriver;

	@Before
	public void _before() throws ArangoException {
		removeTestDatabase(DATABASE_NAME);

		arangoDriver = getArangoDriver(getConfiguration());
		createDatabase(arangoDriver, DATABASE_NAME);
		createGraph(arangoDriver, GRAPH_NAME, EDGE_COLLECTION_NAME, VERTEXT_COLLECTION_NAME);
		addExampleElements();
	}

	@Test
	public void graphAqlQuery() throws ArangoException {

		//
		printHeadline("get all vertices");
		//

		int count = 0;
		VertexCursor<Person> vertexCursor = arangoDriver.graphGetVertexCursor(GRAPH_NAME, Person.class, null, null,
			null);
		Assert.assertNotNull(vertexCursor);

		Iterator<VertexEntity<Person>> vertexIterator = vertexCursor.iterator();
		while (vertexIterator.hasNext()) {
			VertexEntity<Person> entity = vertexIterator.next();
			Assert.assertNotNull(entity);

			Person person = entity.getEntity();
			Assert.assertNotNull(person);

			System.out.printf("%d\t%20s  %15s%n", ++count, entity.getDocumentHandle(), person.getName());
		}
		Assert.assertEquals(6, count);

		//
		printHeadline("get vertices by example");
		//

		count = 0;
		vertexCursor = arangoDriver.graphGetVertexCursor(GRAPH_NAME, Person.class, new Person("Christoph", null), null,
			null);
		Assert.assertNotNull(vertexCursor);

		vertexIterator = vertexCursor.iterator();
		while (vertexIterator.hasNext()) {
			VertexEntity<Person> entity = vertexIterator.next();
			Assert.assertNotNull(entity);

			Person person = entity.getEntity();

			Assert.assertNotNull(person);
			System.out.printf("%d\t%20s  %15s%n", ++count, entity.getDocumentHandle(), person.getName());
		}
		Assert.assertEquals(1, count);

		//
		printHeadline("get all edges");
		//

		count = 0;
		EdgeCursor<Knows> edgeCursor = arangoDriver.graphGetEdgeCursor(GRAPH_NAME, Knows.class, null, null, null);
		Assert.assertNotNull(edgeCursor);

		Iterator<EdgeEntity<Knows>> edgeIterator = edgeCursor.iterator();
		while (edgeIterator.hasNext()) {
			EdgeEntity<Knows> entity = edgeIterator.next();
			Assert.assertNotNull(entity);

			Knows knows = entity.getEntity();
			Assert.assertNotNull(knows);

			System.out.printf("%d\t%20s  %20s->%20s %d%n", ++count, entity.getDocumentHandle(),
				entity.getFromVertexHandle(), entity.getToVertexHandle(), knows.getSince());
		}
		Assert.assertEquals(7, count);

		//
		printHeadline("get all outbound edges of a vertex");
		//

		count = 0;
		edgeCursor = arangoDriver.graphGetEdgeCursor(GRAPH_NAME, Knows.class, new Person("Christoph", Person.MALE),
			new GraphEdgesOptions().setDirection(Direction.OUTBOUND), null);
		Assert.assertNotNull(edgeCursor);

		edgeIterator = edgeCursor.iterator();
		while (edgeIterator.hasNext()) {
			EdgeEntity<Knows> entity = edgeIterator.next();
			Assert.assertNotNull(entity);

			Knows knows = entity.getEntity();
			Assert.assertNotNull(knows);
			System.out.printf("%d\t%20s  %20s->%20s %d%n", ++count, entity.getDocumentHandle(),
				entity.getFromVertexHandle(), entity.getToVertexHandle(), knows.getSince());
		}
		Assert.assertEquals(2, count);

		//
		printHeadline("get shortest path");
		//

		// path Anton -> Frauke
		ShortestPathEntity<Person, Knows> shortestPath = arangoDriver.graphGetShortestPath(GRAPH_NAME,
			new Person("Anton", Person.MALE), new Person("Frauke", Person.FEMALE),
			new ShortestPathOptions().setDirection(Direction.OUTBOUND), Person.class, Knows.class);
		Assert.assertNotNull(shortestPath);

		printShortestPath(shortestPath);
		Assert.assertEquals(new Long(2L), shortestPath.getDistance());

		//
		printHeadline("get shortest path (empty)");
		//

		// path Frauke -> Anton (empty result)
		shortestPath = arangoDriver.graphGetShortestPath(GRAPH_NAME, new Person("Frauke", Person.FEMALE),
			new Person("Anton", Person.MALE), new ShortestPathOptions().setDirection(Direction.OUTBOUND), Person.class,
			Knows.class);
		Assert.assertNotNull(shortestPath);

		printShortestPath(shortestPath);
		// no path found (distance = -1)
		Assert.assertEquals(new Long(-1), shortestPath.getDistance());

	}

	private void printShortestPath(ShortestPathEntity<Person, Knows> shortestPath) {
		if (shortestPath.getDistance() > -1) {
			System.out.printf("%s -> %s : distance = %d, getVertices().size() = %d, edges.size() = %d%n",
				shortestPath.getVertices().get(0).getDocumentHandle(),
				shortestPath.getVertices().get(shortestPath.getVertices().size() - 1).getDocumentHandle(),
				shortestPath.getDistance(), shortestPath.getVertices().size(), shortestPath.getEdges().size());
		} else {
			System.out.println("no path");
		}
	}

	private void addExampleElements() throws ArangoException {
		// create some persons
		VertexEntity<Person> v1 = arangoDriver.graphCreateVertex(GRAPH_NAME, VERTEXT_COLLECTION_NAME,
			new Person("Anton", Person.MALE), true);
		VertexEntity<Person> v2 = arangoDriver.graphCreateVertex(GRAPH_NAME, VERTEXT_COLLECTION_NAME,
			new Person("Boris", Person.MALE), false);
		VertexEntity<Person> v3 = arangoDriver.graphCreateVertex(GRAPH_NAME, VERTEXT_COLLECTION_NAME,
			new Person("Christoph", Person.MALE), false);
		VertexEntity<Person> v4 = arangoDriver.graphCreateVertex(GRAPH_NAME, VERTEXT_COLLECTION_NAME,
			new Person("Doris", Person.FEMALE), false);
		VertexEntity<Person> v5 = arangoDriver.graphCreateVertex(GRAPH_NAME, VERTEXT_COLLECTION_NAME,
			new Person("Else", Person.FEMALE), false);
		VertexEntity<Person> v6 = arangoDriver.graphCreateVertex(GRAPH_NAME, VERTEXT_COLLECTION_NAME,
			new Person("Frauke", Person.FEMALE), false);

		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v1.getDocumentHandle(), v2.getDocumentHandle(),
			new Knows(1984), false);
		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v2.getDocumentHandle(), v6.getDocumentHandle(),
			new Knows(1990), false);
		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v1.getDocumentHandle(), v3.getDocumentHandle(),
			new Knows(1992), false);
		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v3.getDocumentHandle(), v4.getDocumentHandle(),
			new Knows(1996), false);
		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v4.getDocumentHandle(), v6.getDocumentHandle(),
			new Knows(2003), false);
		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v4.getDocumentHandle(), v5.getDocumentHandle(),
			new Knows(2013), false);
		arangoDriver.graphCreateEdge(GRAPH_NAME, EDGE_COLLECTION_NAME, v3.getDocumentHandle(), v5.getDocumentHandle(),
			new Knows(2003), false);
	}

}
