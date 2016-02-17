// http://www.matlus.com/html5-file-upload-with-progress/


// main view
$(document).ready( function() {
(function ($) {
	var EventBus = _.extend({}, Backbone.Events);

	P2PFile = Backbone.Model.extend({
	  defaults: {
  		"pathname": null,
  		"name": null,
  		"owner": null,
  		"revision": null,
  		"type": null,
  		"status": null
	  }
	});

	P2PFiles = Backbone.Collection.extend({
		model: P2PFile
	});

	FilesView = Backbone.View.extend({
		el: $("#p2pfiles"),
		events: {"click #refreshlist": "methodRefresh"},
		initialize: function () {
			this.collection = new P2PFiles();
			this.collection.bind('add', this.renderAppend);
			this.collection.bind('reset', this.renderClear);
			EventBus.on("newmodel", this.updateList, this);
			EventBus.on("addmodel", this.updateOneItem, this);
			EventBus.on("upload", this.upload, this);
			EventBus.on("uploadFailed", function () { 
				alert("There was an error attempting to upload the file."); 
			}, this);
			this.render();
			this.methodRefresh();
		},
		methodRefresh: function() {
			$("#refreshlist").attr("disabled","disabled");
			
			jQuery.ajax(
				{
					url:"/data",
					type:'GET',
					dataType:'json',
					success:function(data){
						EventBus.trigger("newmodel", data);
					},
					error:function(jqXHR, textStatus, errorThrown)
					{
						$("#refreshlist").removeAttr("disabled");
						$("#localid").text("Server did not respond");
					},
					beforeSend: function (xhr) {
						xhr.setRequestHeader("Accept", "application/json");
					}
				});
		},
		render: function () {
			var self = this;
			$(self.el).append("<div id='filelist' class='filelist'></div>");
			_(this.collection.models).each(function (item) {
				self.appendItem(item);
			}, this);
		},
		updateList: function(data) {
				
				this.collection.reset();
				if (data.keys) {
					//alert("Successful send " +  data.localid);
					$("#localid").text("UID " + data.localid);
					
					for (var i in data.keys) {
						var k = data.keys[i];
						var f = this.fileFromJSON(k);
						this.collection.add(f);
					} // endfor
				}//endif
				
				$("#refreshlist").removeAttr("disabled");
			},
		renderAppend: function (item) {
			$('#filelist', this.el).append("<div class='fileentry'><div class='filename'><a href='" + item.get("pathname") + "'>" + item.get("name") + "</a></div></div>");
		},
		renderClear: function(item) {
			$('#filelist', this.el).empty();
		},
		updateOneItem: function(data) {
			var file = this.fileFromJSON(data);
			if (!_.any(this.collection.models, function (test) { return file.get("name") == test.get("name"); } )) {
				this.collection.add(file );
			}
			// else do some refresh??
		},
		upload: function(file) {
			if (file == null)
			{
				alert("You did not specify any file.");
			}
			if(_.any(this.collection.models, function (test) { return file.name == test.get("name"); } )){
				if (confirm("The file already exists. Are you sure you want to overwrite "  + file.name + "?")== false)
				{
					return;
				}
			}
		
			$('#progressNumber').show();

			var fd = new FormData();
			fd.append(file.name, file);

			jQuery.ajax({
				data:fd,
				type: "POST",
				url: "/data?storeas=mime",
				processData: false,
				contentType: false,
				dataType:"json",
				xhr: function() {
					var xhr = jQuery.ajaxSettings.xhr();
					if(xhr instanceof window.XMLHttpRequest) {
						xhr.upload.addEventListener('progress', function (evt) {
							if (evt.lengthComputable) {
							  var percentComplete = Math.round(evt.loaded * 100 / evt.total);
							  $('#progressNumber').text(percentComplete.toString() + '%');
							}
							else {
							  $('#progressNumber').text('10%');
							} // end if
						}, false);
					}
					return xhr;
				},
				beforeSend: function (xhr) {
					xhr.setRequestHeader("Accept", "application/json");
				},
				success: function (data, textStatus, jqXHR) {
					EventBus.trigger("clearUpload");
					if (data.keys) {
					    var entry = data.keys.find(function(el) {
					        return el.key === "/data/lft/" + file.name;
					    });
					    EventBus.trigger("addmodel", entry);
					}
					$('#progressNumber').hide();
					$("#upload").hide();
				},
				error: function(jqXHR, textStatus, errorThrown) {
					EventBus.trigger("uploadFailed");						
					$('#progressNumber').hide();
					$("#upload").hide();
				}
			});
		
		},
		fileFromJSON: function(k)
		{
			if (k instanceof P2PFile)
				return k;
				
			var f = new P2PFile();
			f.set({
			  "pathname" : k.key,
			  "name" :  k.key.substring(k.key.lastIndexOf("/") + 1),
			  "owner" : k.owner,
			  "revision" : k.revision,
			  "type" : k.type,
			  "status" : k.status 
			});
		
			return f;
		}

	});

	AppView = Backbone.View.extend({
		el: $("#browser"),
		initialize: function () {
			//this.p2pfiles = new P2PFiles( null, {view: this} );
		},
		events: {"click #viewAddfile": "methodAddfile"},
		methodAddfile: function () {
			$("#upload").slideDown();
		}
	});
	
	UploadView = Backbone.View.extend({
		el: $("#upload"),
		initialize: function () {
			$('#progressNumber').hide();
			EventBus.on("clearUpload", this.methodCancel, this);
		},
		events: {"click #btnUploadfile": "methodUploadfile",
				 "change #fileToUpload" : "methodFilePreview",
				 "click #btnCancel" : "methodCancel" },
		methodUploadfile: function() {
			EventBus.trigger("upload", document.getElementById('fileToUpload').files[0]);  
		},
		methodCancel: function() {
			$("#fileToUpload").val("");
			$("#fileName").text('');
			$("#fileSize").text('');
			$("#fileType").text('');

			$("#upload").hide();
		},
		methodFilePreview: function () {
			var file = $("#fileToUpload")[0].files[0]; // jQuery object returned. get the DOM object, then get the file
			if (file) {
			  // display friendly outuput
			  var fileSize = 0;
			  if (file.size > 1024 * 1024)
				fileSize = (Math.round(file.size * 100 / (1024 * 1024)) / 100).toString() + 'MB -- TOO BIG!';
			  else
				fileSize = (Math.round(file.size * 100 / 1024) / 100).toString() + 'KB';
	
			  $("#fileName").text('Name: ' + file.name);
			  $("#fileSize").text('Size: ' + fileSize);
			  $("#fileType").text('Type: ' + file.type);
			} // endif
		  }	// end function
	});

	var appview = new AppView();
	var uploadview = new UploadView();
	var filesview = new FilesView();

})(jQuery);
});
