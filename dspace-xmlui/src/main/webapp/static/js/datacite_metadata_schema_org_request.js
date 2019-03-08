// DATASHARE - start
$(document).ready(function () {
  try {
    // This is a Datashare specific script. Other DSpace users will need to alter the script for their purpose.
    // Look for meta tag like <meta name="DC.identifier" content="https://doi.org/10.7488/ds/2503" scheme="DCTERMS.URI" /> to get DOI
    var doiDcIdentifierElement = $("meta[name='DC.identifier'][content^='https://doi.org/10.7488/ds']");

    // Return if no DOI metadata  tag found
    if (doiDcIdentifierElement.length != 1) {
      return;
    }

    // URLs of live and test Datacite api
    var LIVE_DATACITE_API_URL = "https://api.datacite.org/dois/application/vnd.schemaorg.ld+json";
    var TEST_DATACITE_API_URL = "https://api.test.datacite.org/dois/application/vnd.schemaorg.ld+json";

    var doiString = doiDcIdentifierElement.attr("content");
    var doi = new URL(doiString);

    // Url to get Datacite schema date to populate script used in Google Search
    var url;

    if (doiString.match(/^https:\/\/doi.org\/10.7488\/ds\//)) {
      url = LIVE_DATACITE_API_URL + doi.pathname;
    } else if (doiString.match(/^https:\/\/doi.org\/10.7488\/dsbeta\//)) {
      url = TEST_DATACITE_API_URL + doi.pathname;
    }

    $.ajax({
      url: url,
      dataType: 'text', // don't convert JSON to Javascript object
      success: function (data) {
        $('<script>')
         .attr('type', 'application/ld+json')
         .text(data)
         .appendTo('.container');
      },
      error: function (error) {
        console.log(error.responseJSON);
      }
    });
  }
  catch (e) {
    console.log(e);
  }
});
// DATASHARE - end
