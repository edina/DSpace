package uk.ac.edina.datashare.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

/**
 * A curation job to check if an item's bitstreams are too large to be emailed together 
 * and which of the item's bitstreams cannot be emailed separately because of size.
 * 
 * @author John Pinto
 *
 */

@Distributive
public class EmailableBitstreamsChecker extends AbstractCurationTask {

	private static Logger log = Logger.getLogger(EmailableBitstreamsChecker.class);

	
	private static final String EMAILABLE_BITSTREAMS_MAX_SIZE_IN_MB_KEY = "emailable.bitstreams.max.size";
	private static final int DEFAULT_EMAILABLE_BITSTREAMS_MAX_SIZE_IN_MB = 100;
	private static final long ONE_MEGABYTE_SIZE_IN_BYTES = 1024L * 1024L;

	private static final String DS_METADATA_SCHEMA = "ds";
	private static final String NOT_EMAILABLE_METADATA_ELEMENT = "not-emailable";
	private static final String BITSTREAM_METADATA_QUALIFIER = "bitstream";
	private static final String ITEM_METADATA_QUALIFIER = "item";
	private static final String METADATA_LANGUAGE = "en";
	private static final String TRUE = "true";

	private int status = Curator.CURATE_UNSET;
	private long maxAllowedBitstreamsSizeInBytes;
	
	private long totalBitstreamSize = 0L;

	@Override
	public void init(Curator curator, String taskId) throws IOException {
		super.init(curator, taskId);
		maxAllowedBitstreamsSizeInBytes = 
				ConfigurationManager.getLongProperty(EMAILABLE_BITSTREAMS_MAX_SIZE_IN_MB_KEY,
						DEFAULT_EMAILABLE_BITSTREAMS_MAX_SIZE_IN_MB) * ONE_MEGABYTE_SIZE_IN_BYTES;
	}

	/**
	 * Add metadata for non-emailable bitreams and the corresponding item.
	 *
	 * @param dso
	 *            The DSpaceObject to be checked
	 * @return The curation task status of the checking
	 */
	@Override
	public int perform(DSpaceObject dso) {
		// The results that we'll return
		StringBuilder results = new StringBuilder();

		// Unless this is an item, we'll skip this item
		status = Curator.CURATE_SKIP;
		logDebugMessage("The target dso is " + dso.getName());
		if (dso instanceof Item) {
			boolean itemNotEmailableMetadataSet = false;
			try {
				Item item = (Item) dso;
				item.clearMetadata(DS_METADATA_SCHEMA,
						           NOT_EMAILABLE_METADATA_ELEMENT,
						           ITEM_METADATA_QUALIFIER,
						           METADATA_LANGUAGE);
				for (Bundle bundle : item.getBundles()) {

					if ("ORIGINAL".equals(bundle.getName())) {
						for (Bitstream bitstream : bundle.getBitstreams()) {
							totalBitstreamSize += bitstream.getSize();
							logDebugMessage("The bitstream size (bytes): " + bitstream.getSize());
							if (bitstream.getSize() > maxAllowedBitstreamsSizeInBytes) {
								addItemMetaData(item, itemNotEmailableMetadataSet);
								itemNotEmailableMetadataSet = true;
								addBitstreamMetaData(bitstream);
								bitstream.update();
								
							}
						}
					} else if ("THUMBNAIL".equals(bundle.getName())) {
						for (Bitstream bitstream : bundle.getBitstreams()) {
							totalBitstreamSize += bitstream.getSize();
							logDebugMessage("The bitstream size (bytes): " + bitstream.getSize());
							if (bitstream.getSize() > maxAllowedBitstreamsSizeInBytes) {
								addItemMetaData(item, itemNotEmailableMetadataSet);
								itemNotEmailableMetadataSet = true;
								addBitstreamMetaData(bitstream);
								bitstream.update();
							}
						}
					}
				}
				
				logDebugMessage("The total item's bitstreams size (bytes): " + totalBitstreamSize);
				// If the total size of the bitstreams exceeds the max-size then update the item metadata.
				if (totalBitstreamSize > maxAllowedBitstreamsSizeInBytes) {
					addItemMetaData(item, itemNotEmailableMetadataSet);
					itemNotEmailableMetadataSet = true;
				}
				item.update();
                status = Curator.CURATE_SUCCESS;

			} catch (AuthorizeException ae) {
				// Something went wrong
				logDebugMessage(ae.getMessage());
				status = Curator.CURATE_ERROR;
			} catch (SQLException sqle) {
				// Something went wrong
				logDebugMessage(sqle.getMessage());
				status = Curator.CURATE_ERROR;
			}

		}

		logDebugMessage("About to report: " + results.toString());
		setResult(results.toString());
		report(results.toString());

		return status;
	}

	// Add metadata to item object for a non-emailable item's bitstreamms because of total size.
	private void addItemMetaData(Item item, boolean alreadySet) {
		// Only update if not already set.
		if (alreadySet) {
			item.addMetadata(DS_METADATA_SCHEMA,
					         NOT_EMAILABLE_METADATA_ELEMENT,
					         ITEM_METADATA_QUALIFIER,
					         METADATA_LANGUAGE,
					         TRUE);
		}
	}
	
	// Add metadata to the bitstream object if it is non-emailable.
	private void addBitstreamMetaData(Bitstream bitstream) {
		// Clear metadata field we plan to update
		bitstream.clearMetadata(DS_METADATA_SCHEMA,
                                NOT_EMAILABLE_METADATA_ELEMENT,
                                BITSTREAM_METADATA_QUALIFIER,
                                METADATA_LANGUAGE);
		// Add metadata
		bitstream.addMetadata(DS_METADATA_SCHEMA,
					          NOT_EMAILABLE_METADATA_ELEMENT,
					          BITSTREAM_METADATA_QUALIFIER,
					          METADATA_LANGUAGE,
					          TRUE);
	}

	/**
	 * Debugging logging if required
	 *
	 * @param message
	 *            The message to log
	 */
	private void logDebugMessage(String message) {
		if (log.isDebugEnabled()) {
			log.debug(message);
		}
	}

}
