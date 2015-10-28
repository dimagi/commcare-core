package org.commcare.api.session;

import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.resources.ArchiveFileRoot;
import org.commcare.resources.JavaHttpRoot;
import org.commcare.resources.ResourceManager;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.resources.model.installers.BasicInstaller;
import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.resources.model.installers.MediaInstaller;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.resources.model.installers.XFormInstaller;
import org.commcare.resources.reference.JavaResourceRoot;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.PropertySetter;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.commcare.suite.model.graph.BubbleSeries;
import org.commcare.suite.model.graph.Graph;
import org.commcare.suite.model.graph.XYSeries;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.services.storage.IStorageFactory;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.MD5Hasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathEqExpr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipFile;

/**
 * Created by willpride on 10/27/15.
 */
public class ApiConfigEngine {
    private ResourceTable table;
    private ResourceTable updateTable;
    private ResourceTable recoveryTable;
    private final CommCarePlatform platform;

    private ArchiveFileRoot mArchiveRoot;

    public ApiConfigEngine() {
        this.platform = new CommCarePlatform(2, 25);

        PrototypeFactory.setStaticHasher(new ClassNameHasher());

        String[] classes = {
                "org.javarosa.model.xform.XPathReference",
                "org.javarosa.xpath.XPathConditional"
        };

        PrototypeManager.registerPrototypes(classes);
        PrototypeManager.registerPrototypes(XPathParseTool.xpathClasses);

        String[] classes2 = {
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
                "org.javarosa.core.model.data.MultiPointerAnswerData",
                "org.javarosa.core.model.data.PointerAnswerData",
                "org.javarosa.core.model.data.SelectMultiData",
                "org.javarosa.core.model.data.SelectOneData",
                "org.javarosa.core.model.data.StringData",
                "org.javarosa.core.model.data.TimeData",
                "org.javarosa.core.model.data.UncastData",
                "org.javarosa.core.model.data.helper.BasicDataPointer",
                //"org.javarosa.core.model.Action",
                "org.javarosa.core.model.actions.SetValueAction"
        };
        PrototypeManager.registerPrototypes(classes2);

        String[] prototypes = new String[] {BasicInstaller.class.getName(),
                LocaleFileInstaller.class.getName(),
                SuiteInstaller.class.getName(),
                ProfileInstaller.class.getName(),
                MediaInstaller.class.getName(),
                XFormInstaller.class.getName(),
                Text.class.getName(),
                PropertySetter.class.getName(),
                Graph.class.getName(),
                XYSeries.class.getName(),
                BubbleSeries.class.getName(),
                XPathEqExpr.class.getName(),
                TableLocaleSource.class.getName()};
        PrototypeManager.registerPrototypes(prototypes);

        setRoots();

        table = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility(Resource.class,
                "api", "normalTable"));
        updateTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility(Resource.class,
                "api", "updateTable"));
        recoveryTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility(Resource.class,
                "api", "recoveryTable"));


        //All of the below is on account of the fact that the installers
        //aren't going through a factory method to handle them differently
        //per device.
        StorageManager.forceClear();
        StorageManager.setStorageFactory(new IStorageFactory() {
            public IStorageUtility newStorage(String name, Class type) {
                return new SqliteIndexedStorageUtility(type, "api", type.getSimpleName());
            }

        });

        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(FormDef.STORAGE_KEY,FormDef.class);
        StorageManager.registerStorage("fixture", FormInstance.class);
        //StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
    }

    private void setRoots() {
        ReferenceManager._().addReferenceFactory(new JavaHttpRoot());
        this.mArchiveRoot = new ArchiveFileRoot();
        ReferenceManager._().addReferenceFactory(mArchiveRoot);
        ReferenceManager._().addReferenceFactory(new JavaResourceRoot(this.getClass()));
    }


    public void initFromArchive(String archiveURL) {
        String fileName;
        if(archiveURL.startsWith("http")) {
            fileName = downloadToTemp(archiveURL);
        } else {
            fileName = archiveURL;
        }
        ZipFile zip;
        try {
            zip = new ZipFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String archiveGUID = this.mArchiveRoot.addArchiveFile(zip);

        init("jr://archive/" + archiveGUID + "/profile.ccpr");
    }

    private void init(String profileRef) {
        System.out.println("init profile ref: " + profileRef);
        try {
            installAppFromReference(profileRef);
        } catch (InstallCancelledException e) {
            e.printStackTrace();
        } catch (UnresolvedResourceException e) {
            e.printStackTrace();
        } catch (UnfullfilledRequirementsException e) {
            e.printStackTrace();
        }
    }

    public void installAppFromReference(String profileReference) throws UnresolvedResourceException,
            UnfullfilledRequirementsException, InstallCancelledException {
        ResourceManager.installAppResources(platform, profileReference, this.table, true);
    }

    public void initEnvironment() {
        try {
            Localization.init(true);
            table.initializeResources(platform);
            //Make sure there's a default locale, since the app doesn't necessarily use the
            //localization engine
            Localization.getGlobalLocalizerAdvanced().addAvailableLocale("default");

            Localization.setDefaultLocale("default");

            String newLocale = null;
            for (String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
                if (newLocale == null) {
                    newLocale = locale;
                }
                System.out.println("* " + locale);
            }

            Localization.setLocale(newLocale);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private String downloadToTemp(String resource) {
        try{
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
            HttpURLConnection.setFollowRedirects(true);

            File file = File.createTempFile("commcare_", ".ccz");

            FileOutputStream fos = new FileOutputStream(file);
            StreamsUtil.writeFromInputToOutput(new BufferedInputStream(conn.getInputStream()), fos);
            return file.getAbsolutePath();
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public CommCarePlatform getPlatform(){
        return this.platform;
    }
}
