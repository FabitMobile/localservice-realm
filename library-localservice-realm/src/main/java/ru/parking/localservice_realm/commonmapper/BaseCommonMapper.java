package ru.parking.localservice_realm.commonmapper;


import java.util.ArrayList;
import java.util.List;


public abstract class BaseCommonMapper<InputType, ReturnType> implements CommonMapper<InputType, ReturnType> {

    @Override
    public List<ReturnType> map(List<InputType> inputObjects) {
        List<ReturnType> returnObjects = new ArrayList<>();
        for (InputType inputObject : inputObjects) {
            returnObjects.add(map(inputObject));
        }
        return returnObjects;
    }
}
