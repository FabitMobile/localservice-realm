package ru.fabit.localservicerealm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Provider;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import ru.fabit.localservicerealm.commonmapper.CommonMapper;
import ru.fabit.utils.Optional;

public class LocalServiceImpl implements LocalService {

    private static String TAG = "LocalServiceImpl";

    private final RealmConfiguration realmConfiguration;
    private final Provider<Realm> realmProvider;
    private final SchedulerFactory schedulerFactory;
    private final Map<String, Integer> connectionsCounter;
    private final Map<String, Integer> openedCounter;
    private final Map<String, Integer> closedCounter;
    private final Map<Long, Realm> instances;

    public LocalServiceImpl(Provider<Realm> realmProvider,
                            RealmConfiguration realmConfiguration,
                            SchedulerFactory schedulerFactory) {
        this.realmProvider = realmProvider;
        this.schedulerFactory = schedulerFactory;
        this.realmConfiguration = realmConfiguration;
        this.connectionsCounter = new ConcurrentHashMap<>();
        this.openedCounter = new ConcurrentHashMap<>();
        this.closedCounter = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
    }

    @Override
    public Flowable<Optional<Number>> get(final Class clazz, final Function<RealmQuery, RealmQuery> predicate, final AggregationFunction aggregationFunction, final String nameField) {

        final AtomicReference<Realm> realmRef = new AtomicReference(null);

        Scheduler androidScheduler = schedulerFactory.getScheduler(clazz);

        return Flowable.defer((Callable<Flowable<Optional<Number>>>) () -> {
            final Realm realm = getRealm();
            realmRef.set(realm);
            RealmQuery query = realm.where(clazz);
            if (predicate != null) {
                try {
                    query = predicate.apply(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            RealmResults realmResults = query.findAll();

            Number results = null;
            if (realmResults.size() > 0) {
                switch (aggregationFunction) {
                    case MAX:
                        results = realmResults.max(nameField).doubleValue();
                        break;
                    case MIN:
                        results = realmResults.min(nameField).doubleValue();
                        break;
                    case AVERAGE:
                        results = realmResults.average(nameField);
                        break;
                    case SIZE:
                        results = realmResults.size();
                        break;
                    case SUM:
                        results = realmResults.sum(nameField);
                        break;
                }
                return Flowable.just(new Optional<>(results));
            } else return Flowable.just(new Optional<>((Number) null));
        }).doOnCancel(() -> closeRealm(realmRef.get())).subscribeOn(androidScheduler)
                .unsubscribeOn(androidScheduler);
    }

    public Flowable<Integer> getSize(final Class clazz, final Function<RealmQuery, RealmQuery> predicate) {

        final AtomicReference<Realm> realmRef = new AtomicReference(null);

        Scheduler androidScheduler = schedulerFactory.getScheduler(clazz);

        return Flowable.defer((Callable<Flowable<Integer>>) () -> {
            final Realm realm = getRealm();
            realmRef.set(realm);
            RealmQuery query = realm.where(clazz);
            if (predicate != null) {
                if (predicate != null) {
                    try {
                        query = predicate.apply(query);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Flowable<RealmResults> realmResults = query.findAll().asFlowable();
            return realmResults
                    .filter(RealmResults::isLoaded)
                    .map(results -> results.size());
        }).doOnCancel(() -> closeRealm(realmRef.get())).subscribeOn(androidScheduler)
                .unsubscribeOn(androidScheduler);
    }

    @Override
    public <InputType, ReturnType> Flowable<List<ReturnType>> get(final LocalServiceParams<InputType, ReturnType> localServiceParams) {

        final Map.Entry<String[], Sort[]> sortPair = localServiceParams.getSortPair();
        final CommonMapper<InputType, ReturnType> commonMapper = localServiceParams.getCommonMapper();
        final Function<RealmQuery, RealmQuery> predicate = localServiceParams.getPredicate();
        final AtomicReference<Realm> realmRef = new AtomicReference(null);

        incrementIfExist(openedCounter, localServiceParams.getClazz().getSimpleName());


        Scheduler androidScheduler = schedulerFactory.getScheduler(localServiceParams.getClazz());

        return Flowable.defer((Callable<Flowable<List<RealmModel>>>) () -> {
            final Realm realm = getRealm();
            realmRef.set(realm);
            RealmQuery query = realm.where(localServiceParams.getClazz());
            if (predicate != null) {
                try {
                    query = predicate.apply(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Flowable<RealmResults> realmResults;
            if (sortPair == null) {
                realmResults = query.findAll().asFlowable();
            } else {
                realmResults = query.findAll().sort(sortPair.getKey(), sortPair.getValue()).asFlowable();
            }

            return realmResults
                    .filter(RealmResults::isLoaded)
                    .map(items -> {
                        List<RealmModel> realmModels = realm.copyFromRealm(items);
                        return realmModels;
                    });
        })
                .doOnCancel(() -> {
                    closeRealm(realmRef.get());
                    incrementIfExist(closedCounter, localServiceParams.getClazz().getSimpleName());
                })
                .subscribeOn(androidScheduler)
                .unsubscribeOn(androidScheduler)
                .observeOn(Schedulers.io())
                .map(items -> {
                    List<ReturnType> returnModels = commonMapper.map((List<InputType>) items);
                    return returnModels;
                });
    }

    @Override
    public Completable storeObject(final Class clazz,
                                   final JSONObject jsonObject) {

        Scheduler ioScheduler = schedulerFactory.io();

        return Completable.create(emitter -> {
            Realm realm = getRealm();
            realm.beginTransaction();
            try {
                realm.createOrUpdateObjectFromJson(clazz, jsonObject);
            } catch (Exception ex) {
                emitter.onError(ex);
            } finally {
                realm.commitTransaction();
                closeRealm(realm);
                emitter.onComplete();
            }
        }).subscribeOn(ioScheduler);
    }

    @Override
    public Completable storeObjects(final Class clazz,
                                    final JSONArray jsonArray) {

        Scheduler ioScheduler = schedulerFactory.io();

        return Completable.create(emitter -> {
            Realm realm = getRealm();
            realm.beginTransaction();
            try {
                realm.createOrUpdateAllFromJson(clazz, jsonArray);
            } catch (Exception ex) {
                emitter.onError(ex);
            } finally {
                realm.commitTransaction();
                closeRealm(realm);
                emitter.onComplete();
            }
        }).subscribeOn(ioScheduler);
    }

    @Override
    public Completable update(final Class clazz,
                              final Function<RealmQuery, RealmQuery> predicate,
                              final Consumer<RealmModel> action) {

        Scheduler ioScheduler = schedulerFactory.io();

        return Completable.create(emitter -> {
            Realm realm = getRealm();
            realm.beginTransaction();
            RealmQuery query = realm.where(clazz);
            if (predicate != null) {
                query = predicate.apply(query);
            }
            RealmModel realmObject = (RealmModel) query.findFirst();
            action.accept(realmObject);
            if (realmObject != null) {
                realm.copyToRealmOrUpdate(realmObject);
            }
            realm.commitTransaction();
            closeRealm(realm);
            emitter.onComplete();
        }).subscribeOn(ioScheduler);
    }


    @Override
    public Completable delete(final Class clazz,
                              final Function<RealmQuery, RealmQuery> predicate) {

        Scheduler ioScheduler = schedulerFactory.io();

        return Completable.create(emitter -> {
            Realm realm = getRealm();
            try {
                realm.beginTransaction();
                RealmQuery query = realm.where(clazz);
                if (predicate != null) {
                    query = predicate.apply(query);
                }
                RealmResults<RealmModel> results = query.findAll();
                results.deleteAllFromRealm();
            } finally {
                realm.commitTransaction();
                closeRealm(realm);
                emitter.onComplete();
            }
        }).subscribeOn(ioScheduler);
    }

    @Override
    public Completable deleteAndStoreObjects(final Class clazz, final Function<RealmQuery, RealmQuery> predicate, final JSONArray jsonArray) {

        Scheduler ioScheduler = schedulerFactory.io();

        return Completable.create(emitter -> {
            Realm realm = getRealm();
            try {
                realm.beginTransaction();
                RealmQuery query = realm.where(clazz);
                if (predicate != null) {
                    query = predicate.apply(query);
                }
                RealmResults<RealmModel> results = query.findAll();
                results.deleteAllFromRealm();
                realm.createOrUpdateAllFromJson(clazz, jsonArray);
            } catch (Exception ex) {
                emitter.onError(ex);
            } finally {
                realm.commitTransaction();
                closeRealm(realm);
                emitter.onComplete();
            }
        }).subscribeOn(ioScheduler);
    }

    @Override
    public Observable<Set<Integer>> getIds(final Class clazz,
                                           final Function<RealmQuery, RealmQuery> predicate,
                                           final Function<RealmModel, Integer> action) {

        Scheduler ioScheduler = schedulerFactory.io();

        return Observable.create(
                (ObservableOnSubscribe<Set<Integer>>) emitter -> {
                    Realm realm = getRealm();
                    Set<Integer> ids = new HashSet<>();
                    RealmQuery query = realm.where(clazz);
                    if (predicate != null) {
                        query = predicate.apply(query);
                    }
                    RealmResults results = query.findAll();
                    List<RealmModel> realmModels = realm.copyFromRealm(results);
                    for (RealmModel realmModel : realmModels) {
                        int id = action.apply(realmModel);
                        ids.add(id);
                    }
                    closeRealm(realm);
                    emitter.onNext(ids);
                }
        ).subscribeOn(ioScheduler);
    }

    @Override
    public int getGlobalInstanceCount() {
        return Realm.getGlobalInstanceCount(realmConfiguration);
    }

    @Override
    public int getLocalInstanceCount() {
        return Realm.getLocalInstanceCount(realmConfiguration);
    }

    private Realm getRealm() {
        String threadName = Thread.currentThread().getName();
        Long threadId = Thread.currentThread().getId();
        incrementIfExist(connectionsCounter, threadName);
        Realm realm = realmProvider.get();
        if (!instances.containsKey(threadId)) {
            instances.put(threadId, realm);
        }

        return realm;
    }


    private void closeRealm(Realm realm) {
        if (realm != null) {
            String threadName = Thread.currentThread().getName();
            decrementIfExist(connectionsCounter, threadName);
            instances.remove(Thread.currentThread().getId());
            realm.close();
        }
    }

    private void incrementIfExist(Map<String, Integer> map, String key) {
        if (!map.containsKey(key)) {
            map.put(key, 0);
        }

        int value = map.get(key);
        value += 1;
        map.put(key, value);
    }

    private void decrementIfExist(Map<String, Integer> map, String key) {
        if (!map.containsKey(key)) {
            map.put(key, 0);
        }

        int value = map.get(key);
        value -= 1;
        map.put(key, value);
    }


}