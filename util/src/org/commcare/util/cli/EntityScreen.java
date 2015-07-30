/**
 * 
 */
package org.commcare.util.cli;

import org.commcare.api.interfaces.UserDataInterface;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.model.xform.XPathReference;

import java.io.PrintStream;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class EntityScreen extends Screen {
    
    private final int SCREEN_WIDTH = 100;
    
    private TreeReference[] mChoices;
    private String[] rows;
    CommCarePlatform mPlatform;
    SessionWrapper mSession;
    UserDataInterface mSandbox;
    SessionDatum needed;
    String mHeader;
    
    String mTitle;
    
    //TODO: This is now ~entirely generic other than the wrapper, can likely be
    //moved and we can centralize its usage in the other platforms
    @Override
    public void init(CommCarePlatform platform, SessionWrapper session, UserDataInterface sandbox) {
        
        this.mPlatform = platform;
        this.mSandbox = sandbox;
        this.mSession = session;
        
        needed = session.getNeededDatum();
        String detail = needed.getShortDetail();
        if(detail == null) { 
            error("Can't handle entity selection with blank detail definition for datum " + needed.getDataId());
        }
        
        Detail shortDetail = platform.getDetail(detail);
        if(shortDetail == null) {
            error("Missing detail definition for: " + detail);
        }
        
        mTitle = shortDetail.getTitle().evaluate(session.getEvaluationContext()).getName();
        
        mHeader = this.createHeader(shortDetail);
        
        Vector<TreeReference> references = inflateReference(needed.getNodeset());
        
        rows = new String[references.size()];
        
        int i = 0;
        for(TreeReference entity : references) {
            rows[i] = createRow(entity, shortDetail);
            ++i;
        }
        
        
        this.mChoices = new TreeReference[references.size()];
        references.copyInto(mChoices);
        setTitle(mTitle);
    }
    
    private String createRow(TreeReference entity, Detail shortDetail) {
        EvaluationContext context = new EvaluationContext(mSession.getEvaluationContext(), entity);
        
        DetailField[] fields = shortDetail.getFields();
        
        StringBuilder row = new StringBuilder();
        int i = 0;
        for(DetailField field : fields) {
            Object o = field.getTemplate().evaluate(context);
            String s;
            if(!(o instanceof String)) {
                s = "";
            } else {
                s = (String)o;
            }
            
            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getTemplateWidthHint());
            } catch(Exception e) {
                //Really don't care if it didn't work
            }
            addPaddedStringToBuilder(row, s, widthHint);
            i++;
            if(i != fields.length) {
                row.append(" | ");
            }
        }
        return row.toString();
    }
    
    //So annoying how identical this is...
    private String createHeader(Detail shortDetail) {
        EvaluationContext context = mSession.getEvaluationContext();
        
        DetailField[] fields = shortDetail.getFields();
        
        StringBuilder row = new StringBuilder();
        int i = 0;
        for(DetailField field : fields) {
            Object o = field.getHeader().evaluate(context);
            String s;
            if(!(o instanceof String)) {
                s = "";
            } else {
                s = (String)o;
            }
            
            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getHeaderWidthHint());
            } catch(Exception e) {
                //Really don't care if it didn't work
            }
            addPaddedStringToBuilder(row, s, widthHint);
            i++;
            if(i != fields.length) {
                row.append(" | ");
            }
        }
        return row.toString();
    }
    
    
    private void addPaddedStringToBuilder(StringBuilder builder, String s, int width) {
        if(s.length() > width) {
            builder.append(s, 0, width);
            return;
        } 
        builder.append(s);
        if(s.length() == width){
            //nothing
        } else {
            //pad
            for(int i = 0 ; i < width - s.length(); ++i) {
                builder.append(' ');
            }
        }
    }
    
    private String pad(String s, int width) {
        StringBuilder builder = new StringBuilder();
        addPaddedStringToBuilder(builder, s, width);
        return builder.toString();
    }

    private Vector<TreeReference> inflateReference(TreeReference nodeset) {
        EvaluationContext parent = this.mSession.getEvaluationContext();
        return parent.expandReference(nodeset);
    }

    private void setTitle(String titleGuess) {
        String title = this.mTitle;
        if(title == null) {
            try {
                title = Localization.get("app.display.name");
            } catch(NoLocalizedTextException e) {
                //swallow. Unimportant
                title = "CommCare";
            }
        }
        
        String userSuffix = mSandbox.getLoggedInUser() != null ? " | " + mSandbox.getLoggedInUser().getUsername() : ""; 
        
        this.mTitle = title + userSuffix;
    }

    @Override
    public void prompt(PrintStream out) {
        if(this.mTitle != null) {
            out.println(this.mTitle);
            out.println();
        }
        
        out.println(pad("", 5) + mHeader);
        out.println("==============================================================================================");
        
        for(int i =0 ; i < mChoices.length ; ++ i) {
            String d = rows[i];
            out.println(pad(String.valueOf(i), 4) + ")" + d);
        }
    }
    
    public String getValueFromSelection(TreeReference contextRef, SessionDatum needed, EvaluationContext context) {
        // grab the session's (form) element reference, and load it.
        TreeReference elementRef =
            XPathReference.getPathExpr(needed.getValue()).getReference(true);
        
        AbstractTreeElement element =
                context.resolveReference(elementRef.contextualize(contextRef));

        String value = "";
        // get the case id and add it to the intent
        if(element != null && element.getValue() != null) {
            value = element.getValue().uncast().getString();
        }
        return value;
    }

    @Override
    public void updateSession(CommCareSession session, String input) {
        try {  
            int i = Integer.parseInt(input);
            
            String selection = getValueFromSelection(this.mChoices[i], needed, mSession.getEvaluationContext());
            session.setDatum(needed.getDataId(), selection);
        } catch(NumberFormatException e) {
            
        }
    }
}
