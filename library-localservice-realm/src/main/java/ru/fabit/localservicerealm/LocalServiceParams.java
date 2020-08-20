package ru.fabit.localservicerealm;

import java.util.Map;

import io.reactivex.functions.Function;
import io.realm.RealmQuery;
import io.realm.Sort;
import ru.fabit.localservicerealm.commonmapper.CommonMapper;


public class LocalServiceParams<InputType, ReturnType> {

    private Class clazz;
    private Function<RealmQuery, RealmQuery> predicate;
    private Map.Entry<String[], Sort[]> sortPair;
    private CommonMapper<InputType, ReturnType> commonMapper;

    private LocalServiceParams(Builder getRequestBuilder) {
        clazz = getRequestBuilder.clazz;
        predicate = getRequestBuilder.predicate;
        sortPair = getRequestBuilder.sortPair;
        commonMapper = getRequestBuilder.commonMapper;
    }

    public static class Builder<InputType, ReturnType> {

        private Class clazz;
        private Function<RealmQuery, RealmQuery> predicate;
        private Map.Entry<String[], Sort[]> sortPair;
        private CommonMapper<InputType, ReturnType> commonMapper;

        public Builder(Class clazz) {
            this.clazz = clazz;
        }

        public Builder predicate(Function<RealmQuery, RealmQuery> val) {
            predicate = val;
            return this;
        }

        public Builder sortPair(Map.Entry<String[], Sort[]> val) {
            sortPair = val;
            return this;
        }

        public Builder mapper(CommonMapper<InputType, ReturnType> val) {
            commonMapper = val;
            return this;
        }

        public LocalServiceParams<InputType, ReturnType> build() {
            return new LocalServiceParams<>(this);
        }
    }

    public Class getClazz() {
        return clazz;
    }

    public Function<RealmQuery, RealmQuery> getPredicate() {
        return predicate;
    }

    public Map.Entry<String[], Sort[]> getSortPair() {
        return sortPair;
    }

    public CommonMapper<InputType, ReturnType> getCommonMapper() {
        return commonMapper;
    }
}
