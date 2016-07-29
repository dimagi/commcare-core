package org.commcare.resources.model.installers;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.io.StreamsUtil.InputIOException;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 */
public class LocaleFileInstaller implements ResourceInstaller<CommCareInstance> {

    private String locale;
    private String localReference;

    private Hashtable<String, String> cache;

    private static final String valid = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Serialization only!
     */
    @SuppressWarnings("unused")
    public LocaleFileInstaller() {

    }

    public LocaleFileInstaller(String locale) {
        this.locale = locale;
        this.localReference = "";
    }

    @Override
    public boolean initialize(CommCareInstance instance, boolean isUpgrade) throws ResourceInitializationException {
        if (cache == null) {
            Localization.registerLanguageReference(locale, localReference);
        } else {
            Localization.getGlobalLocalizerAdvanced().addAvailableLocale(locale);
            Localization.getGlobalLocalizerAdvanced().registerLocaleResource(locale, new TableLocaleSource(cache));
        }
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException {
        //If we have local resource authority, and the file exists, things are golden. We can just use that file.
        if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
            try {
                if (ref.doesBinaryExist()) {
                    localReference = ref.getURI();
                    table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
                    return true;
                } else {
                    //If the file isn't there, not much we can do about it.
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
            //We need to download the resource, and store it locally. Either in the cache
            //(if no resource location is available) or in a local reference if one exists.
            InputStream incoming = null;
            try {
                if (!ref.doesBinaryExist()) {
                    return false;
                }
                incoming = ref.getStream();
                if (incoming == null) {
                    //if it turns out there isn't actually a remote resource, bail.
                    return false;
                }

                //Now we're gong to try to find a local location to put the resource.
                //Start with an arbitrary file location (since we don't support destination
                //information yet, which we probably should soon).
                String uri = ref.getURI();
                int lastslash = uri.lastIndexOf('/');

                //Now we have a local part reference
                uri = uri.substring(lastslash == -1 ? 0 : lastslash + 1);


                String cleanUri = "";
                //clean the uri ending. NOTE: This should be replaced with a link to a more
                //robust uri cleaning subroutine
                for (int i = 0; i < uri.length(); ++i) {
                    char c = uri.charAt(i);
                    if (valid.indexOf(c) == -1) {
                        cleanUri += "_";
                    } else {
                        cleanUri += c;
                    }
                }

                uri = cleanUri;

                int copy = 0;

                try {
                    Reference destination = ReferenceManager._().DeriveReference("jr://file/" + uri);
                    while (destination.doesBinaryExist()) {
                        //Need a different location.
                        copy++;
                        String newUri = uri + "." + copy;
                        destination = ReferenceManager._().DeriveReference("jr://file/" + newUri);
                    }

                    if (destination.isReadOnly()) {
                        return cache(incoming, r, table, upgrade);
                    }
                    //destination is now a valid local reference, so we can store the file there.

                    OutputStream output = destination.getOutputStream();
                    try {
                        //We're now reading from incoming, so if this fails, we need to ensure that it is closed
                        StreamsUtil.writeFromInputToOutputSpecific(incoming, output);
                    } catch (InputIOException e) {
                        //TODO: This won't necessarily catch issues with the _output)
                        //stream failing. Test for that.
                        throw new UnreliableSourceException(r, e.getMessage());
                    } finally {
                        output.close();
                    }

                    this.localReference = destination.getURI();
                    if (upgrade) {
                        table.commit(r, Resource.RESOURCE_STATUS_UPGRADE);
                    } else {
                        table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
                    }
                    return true;

                } catch (InvalidReferenceException e) {
                    //Local location doesn't exist, put this in the cache
                    return cache(ref.getStream(), r, table, upgrade);
                } catch (IOException e) {
                    //This is a catch-all for local references failing in unexpected ways.
                    return cache(ref.getStream(), r, table, upgrade);
                }
            } catch (IOException e) {
                throw new UnreliableSourceException(r, e.getMessage());
            } finally {
                try {
                    if (incoming != null) {
                        incoming.close();
                    }
                } catch (IOException e) {
                }
            }

            //TODO: Implement local cache code
            //    return false;
        }
        return false;
    }

    private boolean cache(InputStream incoming, Resource r, ResourceTable table, boolean upgrade) throws UnresolvedResourceException {
        //NOTE: Incoming here needs to be _fresh_. It's extremely important that
        //nothing have gotten the stream first

        try {
            cache = LocalizationUtils.parseLocaleInput(incoming);
            table.commit(r, upgrade ? Resource.RESOURCE_STATUS_UPGRADE : Resource.RESOURCE_STATUS_INSTALLED);
            return true;
        } catch (IOException e) {
            throw new UnreliableSourceException(r, e.getMessage());
        } finally {
            try {
                if (incoming != null) {
                    incoming.close();
                }
            } catch (IOException e) {
            }
        }
    }

    @Override
    public boolean upgrade(Resource r) throws UnresolvedResourceException {
        //TODO: Rename file to take off ".N"?
        return true;
    }

    @Override
    public boolean unstage(Resource r, int newStatus) {
        return true;
    }

    @Override
    public boolean revert(Resource r, ResourceTable table) {
        return true;
    }

    @Override
    public int rollback(Resource r) {
        //This does nothing
        return Resource.getCleanFlag(r.getStatus());
    }

    @Override
    public boolean uninstall(Resource r) throws UnresolvedResourceException {
        //If we're not using files, just deal with the cache (this is even likely unnecessary).
        if (cache != null) {
            cache.clear();
            cache = null;
            return true;
        }
        Reference reference;
        try {
            reference = ReferenceManager._().DeriveReference(localReference);
            if (!reference.isReadOnly()) {
                reference.remove();
            }
            //CTS: The table should take care of this for the installer
            //table.removeResource(r);
            return true;
        } catch (InvalidReferenceException e) {
            e.printStackTrace();
            throw new UnresolvedResourceException(r, "Could not resolve locally installed reference at" + localReference);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UnresolvedResourceException(r, "Problem removing local data at reference " + localReference);
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        locale = ExtUtil.readString(in);
        localReference = ExtUtil.readString(in);
        cache = (Hashtable)ExtUtil.nullIfEmpty((Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, locale);
        ExtUtil.writeString(out, localReference);
        ExtUtil.write(out, new ExtWrapMap(ExtUtil.emptyIfNull(cache)));
    }

    @Override
    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problems) {
        try {
            if (locale == null) {
                problems.addElement(new MissingMediaException(r, "Bad metadata, no locale"));
                return true;
            }
            if (cache != null) {
                //If we've gotten the cache into memory, we're fine
            } else {
                try {
                    if (!ReferenceManager._().DeriveReference(localReference).doesBinaryExist()) {
                        throw new MissingMediaException(r, "Locale data does note exist at: " + localReference);
                    }
                } catch (IOException e) {
                    throw new MissingMediaException(r, "Problem reading locale data from: " + localReference);
                } catch (InvalidReferenceException e) {
                    throw new MissingMediaException(r, "Locale reference is invalid: " + localReference);
                }
            }
        } catch (MissingMediaException ure) {
            problems.addElement(ure);
            return true;
        }
        return false;
    }
}
