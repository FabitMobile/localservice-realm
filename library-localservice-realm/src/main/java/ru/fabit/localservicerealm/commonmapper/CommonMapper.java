package ru.fabit.localservicerealm.commonmapper;

import java.util.List;


public interface CommonMapper<InputType, ReturnType> {

    ReturnType map(InputType object);

    List<ReturnType> map(List<InputType> objects);
}