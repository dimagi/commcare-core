package org.javarosa.core.model;

import org.javarosa.core.api.IModule;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.StorageManager;

public class CoreModelModule implements IModule {

    @Override
    public void registerModule() {
        StorageManager.registerStorage(FormDef.STORAGE_KEY, FormDef.class);
        StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);

        String[] classes = {
                "org.javarosa.core.model.SubmissionProfile",
                "org.javarosa.core.model.QuestionDef",
                "org.javarosa.core.model.GroupDef",
                "org.javarosa.core.model.instance.FormInstance",
                "org.javarosa.core.model.instance.ExternalDataInstance",
                "org.javarosa.core.model.data.BooleanData",
                "org.javarosa.core.model.data.DateData",
                "org.javarosa.core.model.data.DateTimeData",
                "org.javarosa.core.model.data.DecimalData",
                "org.javarosa.core.model.data.GeoPointData",
                "org.javarosa.core.model.data.IntegerData",
                "org.javarosa.core.model.data.LongData",
                "org.javarosa.core.model.data.PointerAnswerData",
                "org.javarosa.core.model.data.SelectMultiData",
                "org.javarosa.core.model.data.SelectOneData",
                "org.javarosa.core.model.data.StringData",
                "org.javarosa.core.model.data.TimeData",
                "org.javarosa.core.model.data.UncastData",
                "org.javarosa.core.model.actions.SetValueAction"
        };
        PrototypeManager.registerPrototypes(classes);
    }
}
