package org.commcare.util;

import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.reference.JavaFileRoot;
import org.commcare.modern.reference.JavaHttpRoot;
import org.commcare.modern.reference.JavaResourceRoot;
import org.commcare.resources.ResourceManager;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.TableStateListener;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.PropertySetter;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Suite;
import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageFactory;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathMissingInstanceException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipFile;

/**
 * @author ctsims
 */
public class CommCareConfigEngine {
    private final ResourceTable table;
    private final ResourceTable updateTable;
    private final ResourceTable recoveryTable;
    private final PrintStream print;
    private final CommCarePlatform platform;
    private final PrototypeFactory mLiveFactory;
    
    private ArchiveFileRoot mArchiveRoot;

    public CommCareConfigEngine() {
        this(new LivePrototypeFactory());
    }

    public CommCareConfigEngine(PrototypeFactory prototypeFactory) {
        this(System.out, prototypeFactory);
    }

    public CommCareConfigEngine(OutputStream output, PrototypeFactory prototypeFactory) {
        this.print = new PrintStream(output);
        this.platform = new CommCarePlatform(2, 29);

        this.mLiveFactory = prototypeFactory;

        setRoots();

        table = ResourceTable.RetrieveTable(new DummyIndexedStorageUtility(Resource.class, mLiveFactory));
        updateTable = ResourceTable.RetrieveTable(new DummyIndexedStorageUtility(Resource.class, mLiveFactory));
        recoveryTable = ResourceTable.RetrieveTable(new DummyIndexedStorageUtility(Resource.class, mLiveFactory));


        //All of the below is on account of the fact that the installers
        //aren't going through a factory method to handle them differently
        //per device.
        StorageManager.forceClear();
        StorageManager.setStorageFactory(new IStorageFactory() {
            public IStorageUtility newStorage(String name, Class type) {
                return new DummyIndexedStorageUtility(type, mLiveFactory);
            }

        });

        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(FormDef.STORAGE_KEY,FormDef.class);
        StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);
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
            print.println("File at " + archiveURL + ": is not a valid CommCare Package. Downloaded to: " + fileName);
            e.printStackTrace(print);
            System.exit(-1);
            return;
        }
        String archiveGUID = this.mArchiveRoot.addArchiveFile(zip);

        init("jr://archive/" + archiveGUID + "/profile.ccpr");
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
            print.println("Issue downloading or create stream for " +resource);
            e.printStackTrace(print);
            System.exit(-1);
            return null;
        }
    }

    public void initFromLocalFileResource(String resource) {
        String reference = setFileSystemRootFromResourceAndReturnRelativeRef(resource);

        init(reference);
    }

    private String setFileSystemRootFromResourceAndReturnRelativeRef(String resource) {
        int lastSeparator = resource.lastIndexOf(File.separator);

        String rootPath;
        String filePart;

        if(lastSeparator == -1 ) {
            rootPath = new File("").getAbsolutePath();
            filePart = resource;
        } else {
            //Get the location of the file. In the future, we'll treat this as the resource root
            rootPath = resource.substring(0,resource.lastIndexOf(File.separator));

            //cut off the end
            filePart = resource.substring(resource.lastIndexOf(File.separator) + 1);
        }

        //(That root now reads as jr://file/)
        ReferenceManager._().addReferenceFactory(new JavaFileRoot(rootPath));

        //Now build the testing reference we'll use
        return "jr://file/" + filePart;
    }


    private void init(String profileRef) {
            try {
                installAppFromReference(profileRef);
                print.println("Table resources intialized and fully resolved.");
                print.println(table);
            } catch (InstallCancelledException e) {
                print.println("Install was cancelled by the user or system");
                e.printStackTrace(print);
                System.exit(-1);
            } catch (UnresolvedResourceException e) {
                print.println("While attempting to resolve the necessary resources, one couldn't be found: " + e.getResource().getResourceId());
                e.printStackTrace(print);
                System.exit(-1);
            } catch (UnfullfilledRequirementsException e) {
                print.println("While attempting to resolve the necessary resources, a requirement wasn't met");
                e.printStackTrace(print);
                System.exit(-1);
            }
    }

    public void installAppFromReference(String profileReference) throws UnresolvedResourceException,
            UnfullfilledRequirementsException, InstallCancelledException {
        ResourceManager.installAppResources(platform, profileReference, this.table, true);
    }

    public void initEnvironment() {
        try {
            Localization.init(true);
            table.initializeResources(platform, false);
            //Make sure there's a default locale, since the app doesn't necessarily use the
            //localization engine
            Localization.getGlobalLocalizerAdvanced().addAvailableLocale("default");

            Localization.setDefaultLocale("default");

            print.println("Locales defined: ");
            for (String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
                System.out.println("* " + locale);
            }

            setDefaultLocale();
        } catch (ResourceInitializationException e) {
            print.println("Error while initializing one of the resolved resources");
            e.printStackTrace(print);
            System.exit(-1);
        }
    }

    private void setDefaultLocale() {
        String defaultLocale = "default";
        for (PropertySetter prop : platform.getCurrentProfile().getPropertySetters()) {
            if ("cur_locale".equals(prop.getKey())) {
                defaultLocale = prop.getValue();
                break;
            }
        }
        print.println("Setting locale to: " + defaultLocale);
        Localization.setLocale(defaultLocale);
    }

    public void describeApplication() {
        print.println("Locales defined: ");
        for(String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
            System.out.println("* " + locale);
        }

        Localization.setDefaultLocale("default");

        Vector<Menu> root = new Vector<>();
        Hashtable<String, Vector<Menu>> mapping = new Hashtable<>();
        mapping.put("root",new Vector<Menu>());

        for(Suite s : platform.getInstalledSuites()) {
            for(Menu m : s.getMenus()) {
                if(m.getId().equals("root")) {
                    root.add(m);
                } else {
                    Vector<Menu> menus = mapping.get(m.getRoot());
                    if(menus == null) {
                        menus = new Vector<>();
                    }
                    menus.add(m);
                    mapping.put(m.getRoot(), menus);
                }
            }
        }

        for(String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
            Localization.setLocale(locale);

            print.println("Application details for locale: " + locale);
            print.println("CommCare");

            for(Menu m : mapping.get("root")) {
                print.println("|- " + m.getName().evaluate());
                for(String command : m.getCommandIds()) {
                    for(Suite s : platform.getInstalledSuites()) {
                        if(s.getEntries().containsKey(command)) {
                            print(s,s.getEntries().get(command),2);
                        }
                    }
                }

            }

            for(Menu m : root) {
                for(String command : m.getCommandIds()) {
                    for(Suite s : platform.getInstalledSuites()) {
                        if(s.getEntries().containsKey(command)) {
                            print(s,s.getEntries().get(command),1);
                        }
                    }
                }
            }
        }
    }

    public CommCarePlatform getPlatform() {
        return platform;
    }

    public FormDef loadFormByXmlns(String xmlns) {
        IStorageUtilityIndexed<FormDef> formStorage =
                (IStorageUtilityIndexed)StorageManager.getStorage(FormDef.STORAGE_KEY);
        return formStorage.getRecordForValue("XMLNS", xmlns);
    }

    private void print(Suite s, Entry e, int level) {
        String head = "";
        String emptyhead = "";
        for(int i = 0; i < level; ++i ){
            head +=      "|- ";
            emptyhead += "   ";
        }
        if (e.isView()) {
            print.println(head + "View: " + e.getText().evaluate());
        } else {
            print.println(head + "Entry: " + e.getText().evaluate());
        }
        for(SessionDatum datum : e.getSessionDataReqs()) {
            if(datum instanceof FormIdDatum) {
                print.println(emptyhead + "Form: " + datum.getValue());
            } else if (datum instanceof EntityDatum) {
                String shortDetailId = ((EntityDatum)datum).getShortDetail();
                if(shortDetailId != null) {
                    Detail d = s.getDetail(shortDetailId);
                    try {
                        print.println(emptyhead + "|Select: " + d.getTitle().getText().evaluate(new EvaluationContext(null)));
                    } catch(XPathMissingInstanceException ex) {
                        print.println(emptyhead + "|Select: " + "(dynamic title)");
                    }
                    print.print(emptyhead + "| ");
                    for(DetailField f : d.getFields()) {
                        print.print(f.getHeader().evaluate() + " | ");
                    }
                    print.print("\n");
                }
            }
        }
    }


    final static private class QuickStateListener implements TableStateListener{
        int lastComplete = 0;

        @Override
        public void simpleResourceAdded() {

        }

        @Override
        public void compoundResourceAdded(ResourceTable table) {

        }

        @Override
        public void incrementProgress(int complete, int total) {
            int diff = complete - lastComplete;
            lastComplete = complete;
            for(int i = 0 ; i < diff ; ++i) {
                System.out.print(".");
            }
        }
    }

    /**
     * @param updateTarget Null to request the default latest build. Otherwise a string identifying
     *                     the target of the update:
     *                     'release' - Latest released (or starred) build
     *                     'build' - Latest completed build (released or not)
     *                     'save' - Latest functional saved version of the app
     */
    public void attemptAppUpdate(String updateTarget) {
        ResourceTable global = table;

        // Ok, should figure out what the state of this bad boy is.
        Resource profileRef = global.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);

        Profile profileObj = this.getPlatform().getCurrentProfile();

        global.setStateListener(new QuickStateListener());

        updateTable.setStateListener(new QuickStateListener());

        // When profileRef points is http, add appropriate dev flags
        String authRef = profileObj.getAuthReference();

        try {
            URL authUrl = new URL(authRef);

            // profileRef couldn't be parsed as a URL, so don't worry
            // about adding dev flags to the url's query

            // If we want to be using/updating to the latest build of the
            // app (instead of latest release), add it to the query tags of
            // the profile reference
            if (updateTarget!= null &&
                    ("https".equals(authUrl.getProtocol()) ||
                            "http".equals(authUrl.getProtocol()))) {
                if (authUrl.getQuery() != null) {
                    // If the profileRef url already have query strings
                    // just add a new one to the end
                    authRef = authRef + "&target=" + updateTarget;
                } else {
                    // otherwise, start off the query string with a ?
                    authRef = authRef + "?target" + updateTarget;
                }
            }
        } catch (MalformedURLException e) {
            System.out.print("Warning: Unrecognized URL format: " + authRef);
        }


        try {

            // This populates the upgrade table with resources based on
            // binary files, starting with the profile file. If the new
            // profile is not a newer version, statgeUpgradeTable doesn't
            // actually pull in all the new references

            System.out.println("Checking for updates....");
            ResourceManager resourceManager = new ResourceManager(platform, global, updateTable, recoveryTable);
            resourceManager.stageUpgradeTable(authRef, true);
            Resource newProfile = updateTable.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
            if (!newProfile.isNewer(profileRef)) {
                System.out.println("Your app is up to date!");
                return;
            }

            System.out.println("Update found. New Version: " + newProfile.getVersion());
            System.out.println("Downloading / Preparing Update");
            resourceManager.prepareUpgradeResources();
            System.out.print("Installing update");

            // Replaces global table with temporary, or w/ recovery if
            // something goes wrong
            resourceManager.upgrade();
        } catch(UnresolvedResourceException e) {
            System.out.println("Update Failed! Couldn't find or install one of the remote resources");
            e.printStackTrace();
            return;
        } catch(UnfullfilledRequirementsException e) {
            System.out.println("Update Failed! This CLI host is incompatible with the app");
            e.printStackTrace();
            return;
        } catch(Exception e) {
            System.out.println("Update Failed! There is a problem with one of the resources");
            e.printStackTrace();
            return;
        }

        // Initializes app resources and the app itself, including doing a check to see if this
        // app record was converted by the db upgrader
        initEnvironment();
    }
}
