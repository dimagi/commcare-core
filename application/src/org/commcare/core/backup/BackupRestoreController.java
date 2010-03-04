/**
 * 
 */
package org.commcare.core.backup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Timer;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.util.FileUtility;
import org.javarosa.core.model.util.restorable.Restorable;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.formmanager.view.ISubmitStatusObserver;
import org.javarosa.formmanager.view.transport.TransportResponseProcessor;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledTimerTask;
import org.javarosa.j2me.util.DumpRMS;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.transport.CommUtil;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
import org.javarosa.services.transport.impl.TransportMessageStatus;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.services.transport.senders.SenderThread;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;

/**
 * @author ctsims
 *
 */
public class BackupRestoreController implements HandledCommandListener, ISubmitStatusObserver {

	private String backupURL;
	private String restoreURL;
	private String mode;
	
	private Class snapshotClass;
	private Restorable snapshot;
	
	private BackupRestoreView actionView;
	private BackupRestoreStatusView statusView;
	private int counter;
	private Timer t;
	private TransportMessage activeMsg;
	
	TrivialTransitions listener;
	
	private static final Command svDone = new Command("OK", Command.OK, 1);
	private static final Command avCancel = new Command("Cancel", Command.CANCEL, 1);
	private static final Command avBackup = new Command("Backup", Command.SCREEN, 1);
	private static final Command avRestore = new Command("Restore", Command.SCREEN, 1);
	
	private static final Command backupRaw = new Command("Backup Raw RMS", Command.SCREEN, 2);
	private static final Command restoreRaw = new Command("Restore Raw RMS", Command.SCREEN, 2);

	
	/**
	 * @param backupURL
	 * @param mode
	 * @param restoreURL
	 * @param snapshot
	 */
	public BackupRestoreController(Class snapshotClass, String backupURL, String restoreURL, String mode, BackupRestoreView actionView, BackupRestoreStatusView statusView, TrivialTransitions listener) {
		this.snapshotClass = snapshotClass;
		this.snapshot = (Restorable)PrototypeFactory.getInstance(snapshotClass);
		this.backupURL = backupURL;
		this.mode = mode;
		this.restoreURL = restoreURL;
		
		this.actionView = actionView;
		actionView.addCommand(backupRaw);
		actionView.addCommand(restoreRaw);
		actionView.setCommands(avBackup, avRestore);
		actionView.setCommandListener(this);
		
		this.statusView = statusView;
		statusView.setCommandListener(this);
		
		this.listener = listener;
	}

	private void backup () {
		ByteArrayPayload payload = RestoreUtils.dispatch(snapshot.exportData());
		if ( mode.compareTo(CommCareProperties.BACKUP_MODE_FILE)==0 ){
		    backupToFile(payload);
		} else { // default to HTTP since it's cross-platform compatible
			try {
				backupToServer(payload);
			} catch (Exception e) {
				throw new RuntimeException("exception when backing up: " + e.getClass().getName() + "[" + e.getMessage() + "]");
			}
		}
	}
		
	private	void backupToFile(ByteArrayPayload payload){
        J2MEDisplay.setView(statusView);
        statusView.updateMessage("Backup: Backing up data to " + backupURL);
        
        try {
        	FileConnection file = (FileConnection)Connector.open(backupURL);
        	
        	if (file.exists()) {
        		statusView.updateMessage("Backup aborted: another backup file already exists on this memory card. Delete or move this other backup file before attempting to back up this phone.");
        	} else {
        		FileUtility.writeFile(file, payload.getPayloadStream());
                statusView.updateMessage("Backup successful!");
        	}
        	
        	file.close();
        } catch (IOException e){
        	statusView.updateMessage("Failed to write data to file");
        }
        statusView.addCommand(svDone);
	}
	
    private void backupToServer(ByteArrayPayload payload) throws IOException, TransportException {
        J2MEDisplay.setView(statusView);
        statusView.updateMessage("Backup: Sending data to server");
        
        final SimpleHttpTransportMessage msg = new SimpleHttpTransportMessage(payload.getPayloadStream(), backupURL);
        msg.setCacheable(false);
        activeMsg = msg;
        
		SenderThread thread = TransportService.send(msg);
		thread.addListener(this);
        
        t = new Timer();
        counter = 0;
        t.schedule(new HandledTimerTask () {
            public void _run () {
                counter += 1000;
                int status = msg.getStatus();
                System.out.println("blipb: " + status + " " + counter);
                                
        		if (status != TransportMessageStatus.QUEUED)
        			t.cancel();
            
        		int timeout = (int)(1000. * Math.max(((byte[])msg.getContent()).length / 1500., 60.));
        		String statusStr;
        		switch (status) {
        		case TransportMessageStatus.QUEUED:
        			statusStr = (counter < timeout ? null : Localization.get("sending.status.long"));
        			break;
        		case TransportMessageStatus.SENT:
        			statusStr = new TransportResponseProcessor () {
        				public String getResponseMessage(TransportMessage txMsg) {
        					byte[] response = null;
        					boolean understoodResponse = true;
        					String backupID = null;
      				    	if (txMsg.isSuccess() && txMsg instanceof SimpleHttpTransportMessage) {
    				    		SimpleHttpTransportMessage msg = (SimpleHttpTransportMessage)txMsg; 
    						 
				    			response = msg.getResponseBody();    			
				    			Document doc = CommUtil.getXMLResponse(response);
				    							    			
				    			if (doc != null) {
				    				Element e = doc.getRootElement();
					    			for (int i = 0; i < e.getChildCount(); i++) {
					    				if (e.getType(i) == Element.ELEMENT) {
					    					Element child = e.getElement(i);
					    					if("BackupId".equals(child.getName())) {
					    						backupID = child.getText(0);
					    						break;
					    					}
					    				}
					    			}					    			
				    			} else {
				    				understoodResponse = false;
				    			}
      				    	} else {
      				    		understoodResponse = false;
      				    	}
      				    	
      				    	String retStr;
    				    	if (!understoodResponse) {
    				    		retStr = Localization.get("sending.status.didnotunderstand",
    				    				new String[] {response != null ? CommUtil.getString(response) : "[no response]"});
    				    	} else if (backupID == null) {
    				    		retStr = "Error: data sent, but did not receive valid restore code from server";	
        					} else {
                                retStr = "Backup successful! Your restore code is: " + backupID;
			    			}
    				    	
    				    	return retStr;
    					}
        			}.getResponseMessage(msg);
        			break;
        		case TransportMessageStatus.FAILED:
        			statusStr = Localization.get("sending.status.failed");
        			break;
        		case TransportMessageStatus.CACHED:
        		default:
        			statusStr = Localization.get("sending.status.error");
        			break;
        		}

                if (statusStr != null) {
                	statusView.updateMessage(statusStr);
                    statusView.addCommand(svDone);
                }

            }
        }, 1000, 1000);
    }
    
	public void receiveError(String details) {
		if (activeMsg != null)
			activeMsg.setStatus(TransportMessageStatus.FAILED);
	}

	public void destroy() {
		//do nothing
	}

	public void onChange(TransportMessage message, String remark) {
		//do nothing
	}

	public void onStatusChange(TransportMessage message) {
		//do nothing
	}
    
	private String getRestoreURL (String code) {
		String restURL = restoreURL;
		if (!restURL.endsWith("/")) {
			restURL = restURL + "/";
		}
		restURL = restURL + code;
		return restURL;
	}
	
	private void restore () {
        if ( mode.compareTo(CommCareProperties.BACKUP_MODE_FILE)==0 ){
            restoreFromFile();
        } else { // default to HTTP since it's cross-platform compatible
			try {
	            restoreFromServer();
			} catch (Exception e) {
				throw new RuntimeException("exception when restoring: " + e.getClass().getName() + "[" + e.getMessage() + "]");
			}
        }
	}
	
	private void restoreFromFile(){
        J2MEDisplay.setView(statusView);
        statusView.updateMessage("Restore: Reading backed-up data from " + restoreURL);

        try{ 
            byte[] backupData = FileUtility.getFileDataVerbose( restoreURL );
            processRestore(backupData);
            
            statusView.updateMessage("Restore successful!");
            statusView.addCommand(svDone);  
        } catch (SecurityException e) {
            e.printStackTrace();
            statusView.updateMessage("Error: SecurityException");
            statusView.addCommand(svDone);  
        } catch (IOException e) {
            e.printStackTrace();
            statusView.updateMessage("Error: IOException");
            statusView.addCommand(svDone);  
        } catch (Exception e) {
            e.printStackTrace();
            statusView.updateMessage("Error: Restore: Exception [" + e.getMessage() + "] " + e.getClass().getName());
            statusView.addCommand(svDone);  
        }
	}

    private void restoreFromServer() throws IOException, TransportException {
        String code = actionView.getCode();
//        if (code.length() != 6) {
//            J2MEDisplay.showError("Restore Code", "Enter your 6-digit restore code");
//        }

        statusView.updateMessage("Restore: Attempting to contact server for backed-up data");
        J2MEDisplay.setView(statusView);
        
        final SimpleHttpTransportMessage msg = new SimpleHttpTransportMessage(new ByteArrayInputStream(new byte[] {0x00}), getRestoreURL(code));
        msg.setCacheable(false);
        activeMsg = msg;
        
    	SenderThread thread = TransportService.send(msg);
    	thread.addListener(this);
        
        t = new Timer();
        counter = 0;
        t.schedule(new HandledTimerTask () {
            public void _run () {
                counter += 1000;
                int status = msg.getStatus();
                System.out.println("blipb: " + status + " " + counter);
                                
        		if (status != TransportMessageStatus.QUEUED) {
        			t.cancel();
                } else if (counter > 300000) {
                    status = TransportMessageStatus.FAILED;
                    t.cancel();
                }

                String statusStr;
                switch (status) {
                case TransportMessageStatus.QUEUED:  statusStr = null; break;
                case TransportMessageStatus.SENT:    statusStr = null;
                	statusStr = new TransportResponseProcessor () {
        				public String getResponseMessage(TransportMessage txMsg) {
                			statusView.updateMessage("Restore: Restoring data...");
                			
                			try {
                				processRestore(((SimpleHttpTransportMessage)txMsg).getResponseBody());
                			} catch (Exception e) {
                				return "Error: Restore: Exception [" + e.getMessage() + "] " + e.getClass().getName();
                			}
                			return "Restore: Restore successful!";
        				}
        			}.getResponseMessage(msg);
        			break;
                case TransportMessageStatus.FAILED:  statusStr = "Failed to connect or get data from server"; break;
                case TransportMessageStatus.CACHED:
                default:
                	statusStr = "Unknown error when contacting server"; break;
                }

                if (statusStr != null) {
                    statusView.updateMessage(statusStr);
                    statusView.addCommand(svDone);
                }
            }
        }, 1000, 1000);        
    }
    
    private void processRestore (byte[] data) {
		snapshot.importData(RestoreUtils.receive(data, snapshotClass));
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		if (c == avCancel) {
			listener.done();
		} else if (c == avBackup) {
			backup();
		} else if (c == avRestore) {
			restore();
		} else if (c == svDone) {
			if (t != null) {
				t.cancel();
			}
			listener.done();
		} else if (c == backupRaw) {
			try {
				DumpRMS.dumpRMS((String)null);
				J2MEDisplay.showError("Raw Backup", "RMS backup successful");
			} catch (Exception e) {
				J2MEDisplay.showError("Raw Backup", "RMS backup failed: " + e.getMessage());				
			}
		} else if (c == restoreRaw) {
			//still need to work out details before this can be supported safely:
			//  1) should require confirmation that you're about to clobber the whole RMS
			//  2) should immediately quit app as the current context may no longer be valid
			
//			try {
//				DumpRMS.restoreRMS(null);
//				alert("Raw Restore", "RMS restore successful");
//			} catch (Exception e) {
//				alert("Raw Restore", "RMS restore failed: " + e.getMessage() + " (warning: app RMS may now be corrupted)");				
//			}

			J2MEDisplay.showError("Raw Restore", "Not supported yet");
		}
	}


	
}
