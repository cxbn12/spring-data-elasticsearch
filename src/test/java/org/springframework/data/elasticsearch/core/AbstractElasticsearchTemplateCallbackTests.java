/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.event.AfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.AfterSaveCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * @author Roman Puchkovskiy
 */
abstract class AbstractElasticsearchTemplateCallbackTests {

	protected AbstractElasticsearchTemplate template;

	@Mock protected SearchResponse searchResponse;
	@Mock protected org.elasticsearch.search.SearchHit searchHit;

	private final IndexCoordinates index = IndexCoordinates.of("index");

	@Spy private ValueCapturingAfterSaveCallback afterSaveCallback = new ValueCapturingAfterSaveCallback();
	@Spy private ValueCapturingAfterConvertCallback afterConvertCallback = new ValueCapturingAfterConvertCallback();

	protected final void initTemplate(AbstractElasticsearchTemplate template) {
		this.template = template;

		this.template.setEntityCallbacks(EntityCallbacks.create(afterSaveCallback, afterConvertCallback));
	}

	protected final org.elasticsearch.search.SearchHits nSearchHits(int count) {
		org.elasticsearch.search.SearchHit[] hits = new org.elasticsearch.search.SearchHit[count];
		Arrays.fill(hits, searchHit);
		return new org.elasticsearch.search.SearchHits(hits, new TotalHits(count, TotalHits.Relation.EQUAL_TO), 1.0f);
	}

	@Test // DATAES-771
	void saveOneShouldInvokeAfterSaveCallbacks() {

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity);

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		Person entity = new Person("init", "luke");

		Person saved = template.save(entity, index);

		verify(afterSaveCallback).onAfterSave(eq(entity));
		assertThat(saved.id).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveArrayShouldInvokeAfterSaveCallbacks() {

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(entity1, entity2);

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveIterableShouldInvokeAfterSaveCallbacks() {

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2));

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void saveIterableWithIndexCoordinatesShouldInvokeAfterSaveCallbacks() {

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		Iterable<Person> saved = template.save(Arrays.asList(entity1, entity2), index);

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Iterator<Person> savedIterator = saved.iterator();
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
		assertThat(savedIterator.next().getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void indexShouldInvokeAfterSaveCallbacks() {

		Person entity = new Person("init", "luke");

		IndexQuery indexQuery = indexQueryForEntity(entity);
		template.index(indexQuery, index);

		verify(afterSaveCallback).onAfterSave(eq(entity));
		Person savedPerson = (Person) indexQuery.getObject();
		assertThat(savedPerson.id).isEqualTo("after-save");
	}

	private IndexQuery indexQueryForEntity(Person entity) {
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setObject(entity);
		return indexQuery;
	}

	@Test // DATAES-771
	void bulkIndexShouldInvokeAfterSaveCallbacks() {

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		IndexQuery query1 = indexQueryForEntity(entity1);
		IndexQuery query2 = indexQueryForEntity(entity2);
		template.bulkIndex(Arrays.asList(query1, query2), index);

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Person savedPerson1 = (Person) query1.getObject();
		Person savedPerson2 = (Person) query2.getObject();
		assertThat(savedPerson1.getId()).isEqualTo("after-save");
		assertThat(savedPerson2.getId()).isEqualTo("after-save");
	}

	@Test // DATAES-771
	void bulkIndexWithOptionsShouldInvokeAfterSaveCallbacks() {

		Person entity1 = new Person("init1", "luke1");
		Person entity2 = new Person("init2", "luke2");

		IndexQuery query1 = indexQueryForEntity(entity1);
		IndexQuery query2 = indexQueryForEntity(entity2);
		template.bulkIndex(Arrays.asList(query1, query2), BulkOptions.defaultOptions(), index);

		verify(afterSaveCallback, times(2)).onAfterSave(any());
		Person savedPerson1 = (Person) query1.getObject();
		Person savedPerson2 = (Person) query2.getObject();
		assertThat(savedPerson1.getId()).isEqualTo("after-save");
		assertThat(savedPerson2.getId()).isEqualTo("after-save");
	}

	@Test // DATAES-772
	void getShouldInvokeAfterConvertCallback() {

		Person result = template.get("init", Person.class);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(result.id).isEqualTo("after-convert");
	}

	private Document lukeDocument() {
		return Document.create().append("id", "init").append("firstname", "luke");
	}

	@Test // DATAES-772
	void getWithCoordinatesShouldInvokeAfterConvertCallback() {

		Person result = template.get("init", Person.class, index);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), eq(index));
		assertThat(result.id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void getViaQueryShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		Person result = template.get(new GetQuery("init"), Person.class, index);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), eq(index));
		assertThat(result.id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void multiGetShouldInvokeAfterConvertCallback() {

		List<Person> results = template.multiGet(queryForTwo(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.get(0).id).isEqualTo("after-convert");
		assertThat(results.get(1).id).isEqualTo("after-convert");
	}

	private Query queryForTwo() {
		return new NativeSearchQueryBuilder().withIds(Arrays.asList("init1", "init2")).build();
	}

	@Test // DATAES-772
	void queryForObjectShouldInvokeAfterConvertCallback() {

		doReturn(nSearchHits(1)).when(searchResponse).getHits();

		@SuppressWarnings("deprecation") // we know what we test
		Person result = template.queryForObject(queryForOne(), Person.class, index);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), eq(index));
		assertThat(result.id).isEqualTo("after-convert");
	}

	private Query queryForOne() {
		return new NativeSearchQueryBuilder().withIds(singletonList("init")).build();
	}

	@Test // DATAES-772
	void queryForPageShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		AggregatedPage<Person> results = template.queryForPage(queryForTwo(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.getContent().get(0).id).isEqualTo("after-convert");
		assertThat(results.getContent().get(1).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void queryForPageWithMultipleQueriesAndSameEntityClassShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		List<Page<Person>> results = template.queryForPage(singletonList(queryForTwo()), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<Person> persons = results.get(0).getContent();
		assertThat(persons.get(0).id).isEqualTo("after-convert");
		assertThat(persons.get(1).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void queryForPageWithMultipleQueriesAndEntityClassesShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		List<AggregatedPage<?>> results = template.queryForPage(singletonList(queryForTwo()), singletonList(Person.class),
				index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<Person> persons = results.get(0).getContent().stream().map(Person.class::cast).collect(Collectors.toList());
		assertThat(persons.get(0).id).isEqualTo("after-convert");
		assertThat(persons.get(1).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void streamShouldInvokeAfterConvertCallback() {

		CloseableIterator<Person> results = template.stream(queryForTwo(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.next().id).isEqualTo("after-convert");
		assertThat(results.next().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchScrollContinueShouldInvokeAfterConvertCallback() {

		CloseableIterator<Person> results = template.stream(queryForTwo(), Person.class, index);

		skipItemsFromScrollStart(results);
		assertThat(results.next().id).isEqualTo("after-convert");
		assertThat(results.next().id).isEqualTo("after-convert");

		verify(afterConvertCallback, times(4)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
	}

	private void skipItemsFromScrollStart(CloseableIterator<Person> results) {
		results.next();
		results.next();
	}

	@Test // DATAES-772
	void queryForListShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		List<Person> results = template.queryForList(queryForTwo(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.get(0).id).isEqualTo("after-convert");
		assertThat(results.get(1).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void queryForListWithMultipleQueriesAndSameEntityClassShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		List<List<Person>> results = template.queryForList(singletonList(queryForTwo()), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<Person> persons = results.get(0);
		assertThat(persons.get(0).id).isEqualTo("after-convert");
		assertThat(persons.get(1).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void queryForListWithMultipleQueriesAndEntityClassesShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		List<List<?>> results = template.queryForList(singletonList(queryForTwo()), singletonList(Person.class), index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<Person> persons = results.get(0).stream().map(Person.class::cast).collect(Collectors.toList());
		assertThat(persons.get(0).id).isEqualTo("after-convert");
		assertThat(persons.get(1).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void moreLikeThisShouldInvokeAfterConvertCallback() {

		@SuppressWarnings("deprecation") // we know what we test
		AggregatedPage<Person> results = template.moreLikeThis(moreLikeThisQuery(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.getContent().get(0).id).isEqualTo("after-convert");
		assertThat(results.getContent().get(1).id).isEqualTo("after-convert");
	}

	private MoreLikeThisQuery moreLikeThisQuery() {
		MoreLikeThisQuery query = new MoreLikeThisQuery();
		query.setId("init");
		query.addFields("id");
		return query;
	}

	@Test // DATAES-772
	void searchOneShouldInvokeAfterConvertCallback() {

		doReturn(nSearchHits(1)).when(searchResponse).getHits();

		SearchHit<Person> result = template.searchOne(queryForOne(), Person.class);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(result.getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchOneWithIndexCoordinatesShouldInvokeAfterConvertCallback() {

		doReturn(nSearchHits(1)).when(searchResponse).getHits();

		SearchHit<Person> result = template.searchOne(queryForOne(), Person.class, index);

		verify(afterConvertCallback).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), eq(index));
		assertThat(result.getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void multiSearchShouldInvokeAfterConvertCallback() {

		List<SearchHits<Person>> results = template.multiSearch(singletonList(queryForTwo()), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<SearchHit<Person>> hits = results.get(0).getSearchHits();
		assertThat(hits.get(0).getContent().id).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void multiSearchWithMultipleEntityClassesShouldInvokeAfterConvertCallback() {

		List<SearchHits<?>> results = template.multiSearch(singletonList(queryForTwo()), singletonList(Person.class),
				index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<? extends SearchHit<?>> hits = results.get(0).getSearchHits();
		assertThat(((Person) hits.get(0).getContent()).id).isEqualTo("after-convert");
		assertThat(((Person) hits.get(1).getContent()).id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchShouldInvokeAfterConvertCallback() {

		SearchHits<Person> results = template.search(queryForTwo(), Person.class);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		List<SearchHit<Person>> hits = results.getSearchHits();
		assertThat(hits.get(0).getContent().id).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchWithIndexCoordinatesShouldInvokeAfterConvertCallback() {

		SearchHits<Person> results = template.search(queryForTwo(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<SearchHit<Person>> hits = results.getSearchHits();
		assertThat(hits.get(0).getContent().id).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchViaMoreLikeThisShouldInvokeAfterConvertCallback() {

		SearchHits<Person> results = template.search(moreLikeThisQuery(), Person.class);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		List<SearchHit<Person>> hits = results.getSearchHits();
		assertThat(hits.get(0).getContent().id).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchViaMoreLikeThisWithIndexCoordinatesShouldInvokeAfterConvertCallback() {

		SearchHits<Person> results = template.search(moreLikeThisQuery(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		List<SearchHit<Person>> hits = results.getSearchHits();
		assertThat(hits.get(0).getContent().id).isEqualTo("after-convert");
		assertThat(hits.get(1).getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchForStreamShouldInvokeAfterConvertCallback() {

		SearchHitsIterator<Person> results = template.searchForStream(queryForTwo(), Person.class);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()), any());
		assertThat(results.next().getContent().id).isEqualTo("after-convert");
		assertThat(results.next().getContent().id).isEqualTo("after-convert");
	}

	@Test // DATAES-772
	void searchForStreamWithIndexCoordinatesShouldInvokeAfterConvertCallback() {

		SearchHitsIterator<Person> results = template.searchForStream(queryForTwo(), Person.class, index);

		verify(afterConvertCallback, times(2)).onAfterConvert(eq(new Person("init", "luke")), eq(lukeDocument()),
				eq(index));
		assertThat(results.next().getContent().id).isEqualTo("after-convert");
		assertThat(results.next().getContent().id).isEqualTo("after-convert");
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Person {

		@Id String id;
		String firstname;
	}

	static class ValueCapturingEntityCallback<T> {

		private final List<T> values = new ArrayList<>(1);

		protected void capture(T value) {
			values.add(value);
		}

		public List<T> getValues() {
			return values;
		}

		@Nullable
		public T getValue() {
			return CollectionUtils.lastElement(values);
		}

	}

	static class ValueCapturingAfterSaveCallback extends ValueCapturingEntityCallback<Person>
			implements AfterSaveCallback<Person> {

		@Override
		public Person onAfterSave(Person entity) {

			capture(entity);
			return new Person() {
				{
					id = "after-save";
					firstname = entity.firstname;
				}
			};
		}
	}

	static class ValueCapturingAfterConvertCallback extends ValueCapturingEntityCallback<Person>
			implements AfterConvertCallback<Person> {

		@Override
		public Person onAfterConvert(Person entity, Document document, IndexCoordinates indexCoordinates) {

			capture(entity);
			return new Person() {
				{
					id = "after-convert";
					firstname = entity.firstname;
				}
			};
		}
	}
}
