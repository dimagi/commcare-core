/**
 *
 */
package org.commcare.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.commcare.cases.CaseManagementModule;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.resources.model.installers.BasicInstaller;
import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.resources.model.installers.MediaInstaller;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.resources.model.installers.XFormInstaller;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.PropertySetter;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.ResourceFileDataSource;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.services.storage.IStorageFactory;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathMissingInstanceException;

/**
 * @author ctsims
 *
 */
public class CommCareConfigEngine {
    private OutputStream output;
    private ResourceTable table;
    private PrintStream print;
    private CommCarePlatform platform;
    private Vector<Suite> suites;
    private Profile profile;
    private int fileuricount = 0;
    
    ArchiveFileRoot mArchiveRoot;

    private void initModules()
    {
        new CoreModelModule().registerModule();
        new XFormsModule().registerModule();
        new CaseManagementModule().registerModule();
        String[] prototypes = new String[] {
                ResourceFileDataSource.class.getName(),
                TableLocaleSource.class.getName(),

                BasicInstaller.class.getName(),
                LocaleFileInstaller.class.getName(),
                SuiteInstaller.class.getName(),
                ProfileInstaller.class.getName(),
                MediaInstaller.class.getName(),
                XFormInstaller.class.getName(),
                Text.class.getName(),
                PropertySetter.class.getName()};
        PrototypeManager.registerPrototypes(prototypes);

    }

    public CommCareConfigEngine() {
        this(System.out);
    }

    public CommCareConfigEngine(OutputStream output) {
        this.output = output;
        this.print = new PrintStream(output);
        suites = new Vector<Suite>();
        this.platform = new CommCarePlatform(2, 23);

        setRoots();

        table = ResourceTable.RetrieveTable(new DummyIndexedStorageUtility(ResourceTable.class));


        //All of the below is on account of the fact that the installers
        //aren't going through a factory method to handle them differently
        //per device.
        StorageManager.setStorageFactory(new IStorageFactory() {

            public IStorageUtility newStorage(String name, Class type) {
                return new DummyIndexedStorageUtility(type);
            }

        });

        initModules();


        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(FormDef.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage("fixture", FormInstance.class);
        //StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
    }

    private void setRoots() {
        ReferenceManager._().addReferenceFactory(new JavaHttpRoot());
        
        this.mArchiveRoot = new ArchiveFileRoot();
        
        ReferenceManager._().addReferenceFactory(mArchiveRoot);
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

    public void initFromLocalFileResource(String resource) {
        //Get the location of the file. In the future, we'll treat this as the resource root
        String root = resource.substring(0,resource.lastIndexOf(File.separator));

        //cut off the end
        resource = resource.substring(resource.lastIndexOf(File.separator) + 1);

        //(That root now reads as jr://file/)
        ReferenceManager._().addReferenceFactory(new JavaFileRoot(root));

        //(Now jr://resource/ points there too)
        ReferenceManager._().addRootTranslator(new RootTranslator("jr://resource","jr://file"));

        //(Now jr://resource/ points there too)
        ReferenceManager._().addRootTranslator(new RootTranslator("jr://media","jr://file"));

        //Now build the testing reference we'll use
        String reference = "jr://file/" + resource;
        
        init(reference);
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

    /**
     * super, super hacky for now, gets a jar directory and loads language resources
     * from it.
     * @param pathToResources
     */
    public void addJarResources(String pathToResources) {
        File resources = new File(pathToResources);
        if(!resources.exists() && resources.isDirectory()) {
            throw new RuntimeException("Couldn't find jar resources at " + resources.getAbsolutePath() + " . Please correct the path, or use the -nojarresources flag to skip loading jar resources.");
        }

        fileuricount++;
        String jrroot = "extfile" + fileuricount;
        ReferenceManager._().addReferenceFactory(new JavaFileRoot(new String[] {jrroot}, resources.getAbsolutePath()));

        for(File file : resources.listFiles()) {
            String name = file.getName();
            if(name.endsWith("txt")) {
                ResourceLocation location = new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, "jr://" + jrroot + "/" + name);
                Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
                locations.add(location);
                if(!(name.lastIndexOf("_") < name.lastIndexOf("."))) {
                    //skip it
                } else {
                    String locale = name.substring(name.lastIndexOf("_") + 1, name.lastIndexOf("."));
                    Resource test = new Resource(-2, name, locations, "Internal Strings: " + locale);
                    try {
                        table.addResource(test, new LocaleFileInstaller(locale),null);
                    } catch (StorageFullException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else {
                //we don't support other file types yet
            }
        }
    }


    public void addResource(String reference) {

    }

    public void init(String profileRef) {
            try {
                platform.init(profileRef, this.table, true);
                print.println("Table resources intialized and fully resolved.");
                print.println(table);
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

    public void initEnvironment() {
        try {
            table.initializeResources(platform);
            
            Localization.setDefaultLocale("default");
            
            print.println("Locales defined: ");
            String newLocale = null;
            for(String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
                if(newLocale == null) {
                    newLocale = locale;
                }
                System.out.println("* " + locale);
            }
            
            print.println("Setting locale to: " + newLocale);
            Localization.setLocale(newLocale);
            
            
        } catch (ResourceInitializationException e) {
            print.println("Error while initializing one of the resolved resources");
            e.printStackTrace(print);
            System.exit(-1);
        }
    }

    public void describeApplication() {
        print.println("Locales defined: ");
        for(String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
            System.out.println("* " + locale);
        }

        Localization.setDefaultLocale("default");

        Vector<Menu> root = new Vector<Menu>();
        Hashtable<String, Vector<Menu>> mapping = new Hashtable<String, Vector<Menu>>();
        mapping.put("root",new Vector<Menu>());

        for(Suite s : suites) {
            for(Menu m : s.getMenus()) {
                if(m.getId().equals("root")) {
                    root.add(m);
                } else {
                    Vector<Menu> menus = mapping.get(m.getRoot());
                    if(menus == null) {
                        menus = new Vector<Menu>();
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
                    for(Suite s : suites) {
                        if(s.getEntries().containsKey(command)) {
                            print(s,s.getEntries().get(command),2);
                        }
                    }
                }

            }

            for(Menu m : root) {
                for(String command : m.getCommandIds()) {
                    for(Suite s : suites) {
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
        IStorageUtilityIndexed<FormDef> formStorage = (IStorageUtilityIndexed)StorageManager.getStorage(FormDef.STORAGE_KEY);
        return formStorage.getRecordForValue("XMLNS", xmlns);
    }

    private void print(Suite s, Entry e, int level) {
        String head = "";
        String emptyhead = "";
        for(int i = 0; i < level; ++i ){
            head +=      "|- ";
            emptyhead += "   ";
        }
        print.println(head + "Entry: " + e.getText().evaluate());
        for(SessionDatum datum : e.getSessionDataReqs()) {
            if(datum.getType() == SessionDatum.DATUM_TYPE_FORM) {
                print.println(emptyhead + "Form: " + datum.getValue());
            } else {
                if(datum.getShortDetail() != null) {
                    Detail d = s.getDetail(datum.getShortDetail());
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

    public CommCareInstance getInstance() {
        return platform;
    }
}
