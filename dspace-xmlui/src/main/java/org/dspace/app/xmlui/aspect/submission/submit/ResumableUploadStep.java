package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

public class ResumableUploadStep extends UploadStep{
    
    //private static final Logger log = Logger.getLogger(ResumableUploadStep.class);
    private static final Message T_column_select =
            message("xmlui.Submission.submit.ResumableUploadStep.select");
    private static final Message T_column_status =
            message("xmlui.Submission.submit.ResumableUploadStep.status");
    private static final Message T_column_info =
            message("xmlui.Submission.submit.ResumableUploadStep.info");
    
    
    private static final Message T_delete_message =
            message("xmlui.Submission.submit.ResumableUploadStep.delete");
    private static final Message T_deletesf_message =
            message("xmlui.Submission.submit.ResumableUploadStep.deletesf");
    private static final Message T_deleteunmatch_message =
            message("xmlui.Submission.submit.ResumableUploadStep.deleteunmatch");

    
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    {
        super.addBody(body);
        
        Item item = submission.getItem();
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue?resumable=true";
        Division div = body.addInteractiveDivision("resumable-upload", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);
        
        Division uploadDiv = div.addDivision("submit-file-upload");
        Division drop = uploadDiv.addDivision("resumable-drop", "col-md-12");
        
        Message T_column_delete = message("xmlui.Submission.submit.ResumableUploadStep.delete");
        drop.addPara(T_column_delete);
        
        // progress bar and button
        uploadDiv.addDivision("progress-button");
        Division progressDiv = uploadDiv.addDivision("progress");
        progressDiv.addDivision("progress-bar");
                
        List upload = div.addList("submit-upload-new-list", List.TYPE_SIMPLE);
        submission.getID();
        
        div.addHidden("submit-id").setValue(submission.getID());
        
        Bundle[] bundles = item.getBundles("ORIGINAL");
        Bitstream[] bitstreams = new Bitstream[0];
        if (bundles.length > 0)
        {
            bitstreams = bundles[0].getBitstreams();
        }
        
        Table summary = uploadDiv.addTable("resumable-upload-summary",(bitstreams.length * 2) + 2,7);
        summary.setHead(T_head2);
        
        Row header = summary.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_column0); // primary bitstream
        header.addCellContent(T_column_select);
        header.addCellContent(T_column2); // file name
        header.addCellContent(T_column4); // description
        header.addCellContent(T_column_status);
        header.addCellContent(T_column_info);
        header.addCellContent(T_column_delete);
        
        for (Bitstream bitstream : bitstreams)
        {
            int id = bitstream.getID();
            Row row = summary.addRow("bitstream-" + id, "data", "resumable-bitstream");

            // Add radio-button to select this as the primary bitstream
            Radio primary = row.addCell("primary-" + id, Cell.ROLE_DATA, "file-primary").addRadio("primary_bitstream_id");
            //Radio primary = row.addCell().addRadio("primary_bitstream_id");
            primary.addOption(String.valueOf(id));

            // If this bitstream is already marked as the primary bitstream
            // mark it as such.
            if(bundles[0].getPrimaryBitstreamID() == id) {
                primary.setOptionSelected(String.valueOf(id));
            }

            // select file
            CheckBox select = row.addCell("select-" + id, Cell.ROLE_DATA, "file-select").addCheckBox("select");
            //CheckBox select = row.addCell().addCheckBox("select");
            select.addOption(String.valueOf(id));
            
            String url = makeBitstreamLink(item, bitstream);
            row.addCell().addXref(url, bitstream.getName());
            
            // description
            row.addCell().addText("description-" + id).setValue(bitstream.getDescription());
            
            // status
            row.addCell("status-" + id, Cell.ROLE_DATA, "file-status-success");
            
            // info
            Cell info = row.addCell("info-" + id, Cell.ROLE_DATA, "file-info");
            info.addHidden("file-extra-bytes").setValue(String.valueOf(bitstream.getSize()));
            info.addHidden("file-extra-format").setValue(bitstream.getFormatDescription());
            info.addHidden("file-extra-algorithm").setValue(bitstream.getChecksumAlgorithm());
            info.addHidden("file-extra-checksum").setValue(bitstream.getChecksum());
            
            // delete
            row.addCell("delete-" + id, Cell.ROLE_DATA, "file-delete");
        }
        
        Division messages = div.addDivision("text-messages", "hide");
        messages.addHidden("text-delete-msg").setValue(T_delete_message);
        messages.addHidden("text-delete-sf").setValue(T_deletesf_message);
        messages.addHidden("text-delete-unmatch").setValue(T_deleteunmatch_message);
        
        // add standard control/paging buttons
        addControlButtons(upload);
    }
    
    public void addPageMeta(PageMeta pageMeta) throws WingException,
        SAXException, SQLException, AuthorizeException, IOException
    {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("javascript", "static").addContent("static/js/upload-resumable.js");
    }

    @Override
    public List addReviewSection(List reviewList)
            throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        return super.addReviewSection(reviewList);
    }
}
