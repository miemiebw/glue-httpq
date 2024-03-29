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

var HttpQ = function(host, port,poolInteval){
	var _host = host;
	var _port = port;
	var _poolInteval = poolInteval;
	var _clientId  = '';
	var _subs = [];
	var _started = false;
	

	
	this.listen = function(options){
		options['name'] = options['name'];
		options['type'] = options['type'] || 'direct';
		options['handleAs'] = options['handleAs'] || 'json';
		options['onMessage'] = options['onMessage'] || function(text,xhr){};
		options['onError'] = options['onError'] || function(xhr){};
		_subs.push(options);
		if(!_started){
			listenWait();
			_started = true;
		}
	}
	var sendRequest = function(){
		var params = 'clientId='+_clientId;
		
		for(var i=0; i<_subs.length; i++){
			params = params + '&name=' + _subs[i].name;
		}
		
		crossdomain.ajax({
			url : 'http://'+ _host +":" + _port + "/q?" + params,
			type : 'get',
			success : function(text, xhr) {
				if(text != undefined && text != ""){
					var resultData = eval('('+text+')');
					
					if(resultData.op == 'getClientId'){
						_clientId = resultData.result;
					}else if(resultData.op == 'getMessage'){
						if(resultData.result.length != 0){
							for(var j=0; j<resultData.result.length; j++){
								var message = resultData.result[j];
								for(var i=0; i<_subs.length; i++){
									if(_subs[i].name == message.headers['header.routingKey']){
										if( _subs[i].handleAs == 'json'){
											_subs[i].onMessage(eval('('+message.body+')'));
										}else if(_subs[i].handleAs == 'text'){
											_subs[i].onMessage(message.body);
										}
									}
								}
							}
						}
					}
					
					listenWait(poolInteval);
				}else{
					listenWait(5000);
				}
			},
			error : function(xhr) {
				for(var i=0; i<_subs.length; i++){
					_subs[i].onError(xhr);
				}
				listenWait(5000);
			},
			data : null
		});
	};
	
	function listenWait(when) {
		if(when == undefined){
			setTimeout(sendRequest, 0);
		}else{
			setTimeout(sendRequest, when);
		}
	}
	
	this.deliver = function(options){
		options['name'] = options['name'];
		options['type'] = options['type'] || 'direct';
		options['body'] = options['body'];
		
		crossdomain.ajax({
			url : 'http://'+ _host +':' + _port + '/q',
			type : 'post',
			success : function(text, xhr) {
				console.dir(text);
			},
			error : function(xhr) {
				console.dir(xhr.responseText);
			},
			data : 'name='+options['name']+'&type='+options['type']+'&body='+options['body']
		});
	}
	
	
	
}