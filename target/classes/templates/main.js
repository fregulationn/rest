jQuery.noConflict();
jQuery(document).ready(function () {

    jQuery("#btnSubmit").click(function (event) {

        //stop submit the form, we will post it manually.
        event.preventDefault();

        fire_ajax_submit();

    });

});

function fire_ajax_submit() {

    // Get form
    var form = jQuery('#fileUploadForm')[0];

    var data = new FormData(form);

    data.append("CustomField", "This is some extra data, testing");

    jQuery("#btnSubmit").prop("disabled", true);

    jQuery.ajax({
        type: "POST",
        enctype: 'multipart/form-data',
        url: "/api/upload/multi",
        data: data,
        //http://api.jquery.com/jQuery.ajax/
        //http://developer.mozilla.org/en-US/docs/Web/API/FormData/Using_FormData_Objects
        processData: false, //prevent jQuery from automatically transforming the data into a query string
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (data) {

            jQuery("#result").text(data);
            console.log("SUCCESS : ", data);
            jQuery("#btnSubmit").prop("disabled", false);

        },
        error: function (e) {

            jQuery("#result").text(e.responseText);
            console.log("ERROR : ", e);
            jQuery("#btnSubmit").prop("disabled", false);

        }
    });

}