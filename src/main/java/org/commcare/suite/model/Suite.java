package org.commcare.suite.model;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * Suites are containers for a set of actions,
 * detail definitions, and UI information. A suite
 * generally contains a set of form entry actions
 * related to the same case ID, sometimes including
 * referrals.
 *
 * @author ctsims
 */
public class Suite implements Persistable {

    public static final String STORAGE_KEY = "SUITE";

    private int version;
    private int recordId = -1;

    /**
     * Detail id -> Detail Object *
     */
    private Hashtable<String, Detail> details;

    /**
     * Entry id (also the same for menus) -> Entry Object *
     */
    private Hashtable<String, Entry> entries;
    private final HashMap<String, List<Menu>> idToMenus = new HashMap<>();
    private final HashMap<String, List<Menu>> rootToMenus = new HashMap<>();

    private Vector<Menu> menus;
    private Hashtable<String, Endpoint> endpoints;

    @SuppressWarnings("unused")
    public Suite() {

    }

    public Suite(int version, Hashtable<String, Detail> details,
                 Hashtable<String, Entry> entries, Vector<Menu> menus, Hashtable<String, Endpoint> endpoints) {
        this.version = version;
        this.details = details;
        this.entries = entries;
        this.menus = menus;
        this.endpoints = endpoints;
        buildIdToMenus();
    }

    private void buildIdToMenus() {
        for (Menu menu : menus) {

            List<Menu> menusWithId = idToMenus.get(menu.getId());
            if (menusWithId == null) {
                menusWithId = new ArrayList<>();
                idToMenus.put(menu.getId(), menusWithId);
            }
            menusWithId.add(menu);

            List<Menu> menusWithRoot = rootToMenus.get(menu.getRoot());
            if (menusWithRoot == null) {
                menusWithRoot = new ArrayList<>();
                rootToMenus.put(menu.getRoot(), menusWithRoot);
            }
            menusWithRoot.add(menu);
        }
    }

    @Override
    public int getID() {
        return recordId;
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    /**
     * @return The menus which define how to access the actions
     * which are available in this suite.
     */
    public Vector<Menu> getMenus() {
        return menus;
    }

    public List<Menu> getMenusWithId(String id) {
        return idToMenus.get(id);
    }

    public List<Menu> getMenusWithRoot(String root) {
        return rootToMenus.containsKey(root) ? rootToMenus.get(root) : new ArrayList<Menu>();
    }

    /**
     * WOAH! UNSAFE! Copy, maybe? But this is _wicked_ dangerous.
     *
     * @return The set of entry actions which are defined by this
     * suite, indexed by their id (which is present in the menu
     * definitions).
     */
    public Hashtable<String, Entry> getEntries() {
        return entries;
    }

    public Entry getEntry(String id) {
        return entries.get(id);
    }

    public Endpoint getEndpoint(String id) {
        return endpoints.get(id);
    }

    public Hashtable<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    /**
     * @param id The String ID of a detail definition
     * @return A Detail definition associated with the provided
     * id.
     */
    public Detail getDetail(String id) {
        return details.get(id);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.recordId = ExtUtil.readInt(in);
        this.version = ExtUtil.readInt(in);
        this.details = (Hashtable<String, Detail>)ExtUtil.read(in, new ExtWrapMap(String.class, Detail.class), pf);
        this.entries = (Hashtable)ExtUtil.read(in, new ExtWrapMapPoly(String.class, true), pf);
        this.menus = (Vector<Menu>)ExtUtil.read(in, new ExtWrapList(Menu.class), pf);
        this.endpoints = (Hashtable<String, Endpoint>)ExtUtil.read(in, new ExtWrapMap(String.class, Endpoint.class), pf);
        buildIdToMenus();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeNumeric(out, version);
        ExtUtil.write(out, new ExtWrapMap(details));
        ExtUtil.write(out, new ExtWrapMapPoly(entries));
        ExtUtil.write(out, new ExtWrapList(menus));
        ExtUtil.write(out, new ExtWrapMap(endpoints));
    }
}
