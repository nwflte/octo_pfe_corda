package com.octo.mapper;

import com.octo.exceptions.MappedSchemaNotFoundStateException;
import com.octo.schemas.PersistentDDRObligation;
import com.octo.states.DDRObligationState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class StateMapper {

    public static <T extends QueryableState> PersistentState map(StateAndRef<T> stateAndRef) throws MappedSchemaNotFoundStateException {
        QueryableState state = stateAndRef.getState().getData();
        Iterator<MappedSchema> iterator = state.supportedSchemas().iterator();
        if (!iterator.hasNext())
            throw new MappedSchemaNotFoundStateException("Class " + state.getClass().getSimpleName() + " doesn't have MappedSchema");
        MappedSchema mappedSchema = iterator.next();
        return state.generateMappedObject(mappedSchema);
    }

    public static List<PersistentDDRObligation> map(List<StateAndRef<DDRObligationState>> statesAndRefs) throws MappedSchemaNotFoundStateException {
        return statesAndRefs.stream().map(st -> (PersistentDDRObligation) map(st)).collect(Collectors.toList());
    }
}
