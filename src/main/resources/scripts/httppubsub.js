var crossdomain = (function() {
	function is_iexplorer() {
		return navigator.userAgent.indexOf('MSIE') != -1
	}

	function ajax(options) {
		var url = options['url'];
		var type = options['type'] || 'GET';
		var success = options['success'];
		var error = options['error'];
		var data = options['data'];

		try {
			var xhr = new XMLHttpRequest();
		} catch (e) {
		}

		if (xhr && "withCredentials" in xhr) {
			xhr.open(type, url, true);
		} else if (typeof XDomainRequest != "undefined") {
			xhr = new XDomainRequest();
			xhr.open(type, url);
		} else {
			xhr = null;
		}

		var handle_load = function(event_type) {
			return function(XHRobj) {
				// stupid IExplorer!!!
				var XHRobj = is_iexplorer() ? xhr : XHRobj;

				if (event_type == 'load'
						&& (is_iexplorer() || XHRobj.readyState == 4)
						&& success) {
					success(XHRobj.responseText, XHRobj);
				} else if (error) {
					error(XHRobj);
				}

			}
		};

		try {
			// withCredentials is not supported by IExplorer's
			// XDomainRequest, neither it is supported by flXHR
			// and it has weird behavior anyway
			xhr.withCredentials = false;
		} catch (e) {
		}
		;

		xhr.onload = function(e) {
			handle_load('load')(is_iexplorer() ? e : e.target)
		};
		xhr.onerror = function(e) {
			handle_load('error')(is_iexplorer() ? e : e.target)
		};
		xhr.send(data);

	}

	return {
		ajax : ajax
	}
})();

var HttpSub = function(queueUrl, successCallback, failureCallback,poolInteval) {

	var sendRequest = function(){
		crossdomain.ajax({
			url : queueUrl,
			type : 'get',
			success : function(text, xhr) {
				if(text != undefined && text != ""){
					successCallback(text,xhr);
					listen(poolInteval);
				}else{
					listen(5000);
				}
			},
			error : function(xhr) {
				failureCallback(xhr);
				listen(5000);
			},
			data : {}
		});
	};
	
	function listen(when) {
		if(when == undefined){
			setTimeout(sendRequest, 0);
		}else{
			setTimeout(sendRequest, when);
		}
		
		return this;
	}
	this.listen = listen;
	
}
