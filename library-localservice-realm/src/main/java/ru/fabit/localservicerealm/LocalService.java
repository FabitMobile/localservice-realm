package ru.fabit.localservicerealm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import ru.fabit.utils.Optional;


public interface LocalService {

    Flowable<Optional<Number>> get(final Class clazz,
                                   final Function<RealmQuery, RealmQuery> predicate,
                                   final AggregationFunction aggregationFunction,
                                   final String nameField);

    <InputType, ReturnType> Flowable<List<ReturnType>> get(LocalServiceParams<InputType, ReturnType> localServiceParams);

    Flowable<Integer> getSize(final Class clazz, Function<RealmQuery, RealmQuery> predicate);

    Completable storeObject(Class clazz, JSONObject jsonObject);

    Completable storeObjects(Class clazz, JSONArray jsonArray);

    Completable update(Class clazz, Function<RealmQuery, RealmQuery> predicate, Consumer<RealmModel> action);

    Completable delete(Class clazz, Function<RealmQuery, RealmQuery> predicate);

    Completable deleteAndStoreObjects(Class clazz, Function<RealmQuery, RealmQuery> predicate, JSONArray jsonArray);

    Observable<Set<Integer>> getIds(Class clazz, Function<RealmQuery, RealmQuery> predicate, Function<RealmModel, Integer> action);

    int getGlobalInstanceCount();

    int getLocalInstanceCount();

}