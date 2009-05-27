/**
*
* myAJAX:  My AJAX implemetation ( http://myajax.sourceforge.net/ )
* Copyright (c) 2006 - 2007, Raul IONESCU <raul.ionescu@yahoo.com>, Bucharest, ROMANIA
*
* Special Thanks:     
* Sebastian IACOB  <isebastian07@yahoo.com>,    Bucharest, ROMANIA
* Sebastian VASILE <sebastianvasile@yahoo.com>, Timisoara, ROMANIA
* Tudor BARBU      <tudor@it-base.ro>,          Bucharest, ROMANIA
*
* @package      myAJAX
* @copyright 	Copyright (c) 2006 - 2007, Raul IONESCU.
* @disclaimer   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
*               INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
*               FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
*               IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
*               OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
*               OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.   
* @author 	Raul IONESCU <raul.ionescu@yahoo.com>
* @license      The MIT License. ( http://www.opensource.org/licenses/mit-license.php )
* @version 	6.1.0
* @category 	Javascript functions for AJAX.
* @access 	public
* @notes        Uses some modified functions from jBookmarker project made by Tudor BARBU.
*               (http://www.it-base.ro/blog/posts/javascript/jbookmarker-create-bookmarkable-ajax-apps.html)
* @notes        Uses some slightly modified functions from http://www.dustindiaz.com/top-ten-javascript/ and 
*               http://www.webtoolkit.info/ 
*
* REDISTRIBUTIONS OF FILES MUST RETAIN THE ABOVE COPYRIGHT NOTICE.
*/



/*   jBookmarker copyright notice:   */
/********************************************************************
Copyright (c) 2006 Tudor Barbu (tudor.b@web-spot.ro)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR 
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

**********************************************************************/



/*/////////////////////////////////////////////////////////////////////*/
function $()
/*/////////////////////////////////////////////////////////////////////*/
/* This function it is a modified function from www.dustindiaz.com */
{
try 
        {
         for(var tmp = (arguments.length == 0)?([this]):(arguments), tmpLength = tmp.length, _$  = function(){ return (myAJAX._vars._applyInheritance)?(function(element){ if(element){ if($isString(element)) { if((element = document.getElementById(element)) == null) { return null; } } else { $inheritFromObject(HTMLElement.prototype, element); } if(!element.id) { element.id = 'myAJAX' + $randomHash(); } return element; } else { return null; } }):(function(element){ if(element){ if($isString(element)) { if((element = document.getElementById(element)) == null) { return null; } } if(!element.id) { element.id = 'myAJAX' + $randomHash(); } return element; } else { return null; }});}, elements = [], i = 0, currentElement = tmp[i]; i < tmpLength; currentElement = tmp[++i]){ elements.push(_$()(currentElement)); }
         return (tmpLength == 1)?(elements[0]):(elements);
        }
catch(e) { if(myAJAX.debugMode) { alert('"$" function ERROR: ' + e.message); } return null; }        
};
/*/////////////////////////////////////////////////////////////////////*/
function $inArray(element, array)
/*///Array/////////////////////////////////////////////////////////////*/  
{
try { for(var tmp = (typeof(array) == 'undefined')?(this):(array), i = (tmp.length-1); i>-1; --i) { if(tmp[i] === element) { return true; } } return false; }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $indexOf(element, array)
/*///Array/////////////////////////////////////////////////////////////*/  
{
try { for(var tmp = (typeof(array) == 'undefined')?(this):(array), i = (tmp.length-1); i>-1; --i) { if(tmp[i] === element) { return i; } } return false; }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $createElement(tag, properties, parent)
/*///document//////////////////////////////////////////////////////////*/  
{
var _getHTML = function(tag, properties) 
        {
         tag = tag.$trim().toLowerCase();
         var propertyName, propertyValue, _html = '<' + tag; 
         
         if(!properties.id) { properties.id = 'myAJAX' + $randomHash(); }
         for(propertyName in properties)
                {
                 propertyValue = properties[propertyName];
                 if(typeof(propertyValue) == 'function') { continue; }
                 if($isString(propertyValue)) { _html += ' ' + propertyName + '="' + propertyValue + '"'; }
                 else
                        {
                         if(propertyName == 'style')
                                {
                                 _html += ' style="';
                                 var s, _htmlStyle = [];
                                 for(s in propertyValue) { if($isString(propertyValue[s])) { _htmlStyle.push(s + ': ' + propertyValue[s]);  } }
                                 _html += _htmlStyle.join('; ') + '"';
                                }
                        }
                }
         if(/^br$|^hr$/i.test(tag)) { _html += '/>'; }
         else { _html += '></' + tag + '>'; } 
         return _html;       
        };
        
var _createElement = function(tag, properties) 
        {
         var s, propertyName, propertyValue, element = document.createElement(tag);
         
         for(propertyName in properties)
                {
                 propertyValue = properties[propertyName];
                 if(typeof(propertyValue) == 'function') { continue; }
                 if($isString(propertyValue)) { element.setAttribute(propertyName, propertyValue); }
                 else { if(propertyName == 'style'){ for(s in propertyValue) { if($isString(propertyValue[s])) { element.style[s] = propertyValue[s]; } } } }
                }

         return element;       
        };

var _html = _getHTML(tag, properties);

try 
        { /* IE specific */
          var element = document.createElement(_html); 
          if(element.nodeName.toUpperCase() == tag.toUpperCase()) { if(parent) { parent.appendChild(element); } }
          else { throw new Error('`createElement` error.'); }
        }
catch(e)
        {
         try 
                {
                 var element = _createElement(tag, properties);
                 if(parent) { parent.appendChild(element); }
                }
         catch(e)
                {
                 try 
                        { 
                         if(parent) 
                                { 
                                  parent.innerHTML += _html; 
                                  element = properties.id.$();
                                }
                         else element = null;                                
                        }
                 catch(e) { if(myAJAX.debugMode) { alert('"$createElement" function ERROR: ' + e.message); } return null; }
                }
        }
return element; 
};
/*/////////////////////////////////////////////////////////////////////*/
function $body(f)
/*///Function//////////////////////////////////////////////////////////*/
{
try      { return (f || this).toString().$trim().replace(/.*\{/,'').replace(/\}$/,''); }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $captureEvent(event, hFunction, useCapture, element)
/*///HTMLElement///////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from www.dustindiaz.com */
{
if(typeof(hFunction) != 'function') { throw new Error("$captureEvent: `hFunction` parameter must be a reference to a function!"); }
try { 
     var tmp = $((typeof(element) == 'undefined')?(this):(element));
     useCapture = (typeof(useCapture) == 'undefined')?(false):((navigator.userAgent.indexOf("Opera") == -1)?(new Boolean(useCapture)):(false));
     
     if(tmp.addEventListener) 
        { 
	 tmp.addEventListener(event, hFunction, useCapture);
	 return true;
	}
     else 
        {
         event = 'on' + event;
         if (tmp.attachEvent) 
                { 
                 tmp.attachEvent(event, hFunction);
                 return true;
                }
	 else { 
                if(tmp[event] && (tmp[event].myAJAX === true))
                        {
                         tmp[event].addCallBack(hFunction);
                        }
                else 
                        {
                         tmp[event] = myAJAX._methods._eventCallBackManager(tmp[event]);
                         tmp[event].addCallBack(hFunction);
                        } 
	        return true;
	      }
        }	
    }
catch(e) { throw new Error('$captureEvent: Can not capture event "' + event + '" on "' + tmp + '" element!'); }
}; 
/*/////////////////////////////////////////////////////////////////////*/
function $copyStyle(destinationNode, sourceNode)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
if(typeof(destinationNode) != 'object') { throw new Error("$copyStyle: `destinationNode` parameter must be an object!"); }
var s = $(sourceNode || this);
try { if(s.className) { destinationNode.className = s.className; } } catch(e) { }

var p, sourceNodeStyle = $getStyle(s); 
for(p in sourceNodeStyle) { if((typeof(sourceNodeStyle[p]) != 'function') && (p != 'length') && (p != 'parentRule')) { try { destinationNode.style[p] = sourceNodeStyle[p]; } catch(e) { } } }
return destinationNode.style;
};
/*/////////////////////////////////////////////////////////////////////*/
function $getPosition(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from Tudor Barbu */
{
try { 
     var tmp = $(node || this), x = 0, y = 0; 
     if(tmp.offsetParent)
        { 
         x = tmp.offsetLeft; 
         y = tmp.offsetTop; 
         while(tmp=tmp.offsetParent) 
                { 
                 x += tmp.offsetLeft; 
                 y += tmp.offsetTop; 
                } 
        } 
     return { x:x, y:y };
    }
catch(e) { return null; }            
};
/*/////////////////////////////////////////////////////////////////////*/
function $getStyle(node, property)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try      { 
          var _node      = $(node || this); 
          var _nodeStyle = (window.getComputedStyle)?(window.getComputedStyle(_node, null)):((_node.currentStyle)?(_node.currentStyle):(_node.style)); 
          return (property)?(_nodeStyle[property]):(_nodeStyle); 
         }
catch(e) { return {}; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function $hide(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { $(node || this).style.visibility = 'hidden'; } catch(e) { }
};
/*/////////////////////////////////////////////////////////////////////*/
function $isForm(form)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try {
     var tmp = $(form || this);
     return (((tmp.nodeName) && (tmp.nodeName.toUpperCase() == 'FORM')) || ((typeof(HTMLFormElement) != 'undefined') && (tmp instanceof (HTMLFormElement))));
    }     
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function $isFormWithFileInput(form)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try {
     var tmp = $(form || this);

     if($isForm(tmp)) 
        { for(var i = (tmp.elements.length - 1), currentElement = tmp.elements[i]; i > -1; currentElement = tmp.elements[--i]) { try { if((currentElement.nodeName.toUpperCase() == 'INPUT') && (currentElement.getAttribute('type').toUpperCase() == 'FILE')) { return true; } } catch(e) { } } }
     return false;
    }     
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/  
function $object2QueryString(object)
/*///HTMLElement///////////////////////////////////////////////////////*/ 
{
var tmp                 = object || ((this === window)?(''):(this));
var regExpIsQueryString = new RegExp ('=');
var _object2QueryString = function(variable)
{ /* ----------------------- */
if(variable && (typeof(variable) == 'object'))
        {
         if($isForm(variable))
                {
                 /* it's a form object */
                 var parameters = [];
                 for (var i = 0, variableElementsLength = variable.elements.length, currentElement = variable.elements[i]; i < variableElementsLength; currentElement = variable.elements[++i])
                        {
                         if(currentElement.disabled || ((typeof(currentElement.type) != 'undefined') &&  ((currentElement.type == 'radio') || (currentElement.type == 'checkbox')) && (currentElement.checked == false))) { continue; }
                         parameters.push(currentElement.name + '=' + ((typeof(currentElement.selectedIndex) == 'undefined')?(currentElement.value):(currentElement.options[currentElement.selectedIndex].value)));
                        }
                 return parameters.join('&');
                }
         else /* it's an single form element or other object */
               {
                 if(variable.name && (variable.value || (variable.selectedIndex && variable.options && variable.options[variable.selectedIndex] && variable.options[variable.selectedIndex].value)))
                        return variable.name + '=' + ((variable.selectedIndex == undefined)?((variable.value == undefined)?(typeof(variable.valueOf()) == 'object')?(''):(variable.valueOf()):(variable.value)):(((variable.options == undefined) || (variable.options[variable.selectedIndex] == undefined) || (variable.options[variable.selectedIndex].value == undefined))?(typeof(variable.valueOf()) == 'object')?(''):(variable.valueOf()):(variable.options[variable.selectedIndex].value)));       
                 else   
                        {
                         var parameters = [];
                         var property;
                         var value;
                         
                         for(property in variable)
                                try { 
                                      value = variable[property];   
                                      if(typeof(value) != 'function') { parameters.push(property + '=' + value);  }
                                     } 
                                catch(e) { }
                         return parameters.join('&');      
                        }  
                }
        }
else { return variable; }
};

if($isString(tmp))
        {
         if(regExpIsQueryString.test(tmp)) {return tmp; }
         if((tmp = $(tmp)) != null) { return _object2QueryString(tmp); }
         return tmp;
        }
else    
        {
         if(typeof(tmp) == 'object') { return _object2QueryString(tmp); }
         else                        { return tmp; }
        }        
};
/*/////////////////////////////////////////////////////////////////////*/
function $setOpacity(opacityValue, node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var objectStyle          = $(node || this).style; 
     objectStyle.KhtmlOpacity = objectStyle.MozOpacity = objectStyle.opacity = opacityValue/101; 
     objectStyle.filter       = "alpha(opacity=" + (opacityValue - 1) + ")";
     objectStyle.visibility   = (opacityValue)?('visible'):('hidden');
     return true;
    }
catch(e) { return false; }            
};
/*/////////////////////////////////////////////////////////////////////*/
function $releaseEvent(event, hFunction, useCapture, element)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
if(typeof(hFunction) != 'function') { throw new Error("$releaseEvent: `hFunction` parameter must be a reference to a function!"); }
try { 
     var tmp = $((typeof(element) == 'undefined')?(this):(element));
     useCapture = (typeof(useCapture) == 'undefined')?(false):((navigator.userAgent.indexOf("Opera") == -1)?(new Boolean(useCapture)):(false));
     
     if(tmp.removeEventListener) 
        { 
	 tmp.removeEventListener(event, hFunction, useCapture);
	 return true;
	}
     else 
        {
         event = 'on' + event;
         if (tmp.detachEvent) 
                {
                 return tmp.detachEvent(event, hFunction);
                }
	 else 
                { 
                 if(tmp[event] && (tmp[event].myAJAX === true))
                        {
                          return tmp[event].removeCallBack(hFunction);
                        }
                 tmp[event] = myAJAX._methods._eventCallBackManager(tmp[event]);
                 return tmp[event].removeCallBack(hFunction);
                }
        }	
    }
catch(e) { throw new Error('$releaseEvent: Can not release event "' + event + '" on "' + tmp + '" element!'); }
}; 
/*/////////////////////////////////////////////////////////////////////*/
function $remove(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var _node = $(node || this);
     return _node.parentNode.removeChild(_node);
    }
catch(e) { if(myAJAX.debugMode) { alert('"$remove" function ERROR: ' + e.message); } return null; }            
};
/*/////////////////////////////////////////////////////////////////////*/
function $reset(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var _node = $(node || this);

     if((typeof(_node.selectedIndex) != 'undefined') && _node.options)
        {
         for(var i = (_node.options.length-1); i > -1; --i)
                { _node.options[i] = null; }
         return true;                
        }

     if(typeof(_node.checked) != 'undefined') 
        {
         _node.checked = false; 
         return true;
        }

     _node.value = '';
     return true;     
    }
catch(e) { if(myAJAX.debugMode) { alert('"$reset" function ERROR: ' + e.message); } return false; }            
};
/*/////////////////////////////////////////////////////////////////////*/
function $show(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { $(node || this).style.visibility = 'visible'; } catch(e) { }
};
/*/////////////////////////////////////////////////////////////////////*/
function $toggleDisplay(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var tmp = $(node || this);
     if((typeof(tmp.oldStyleDisplay) == 'undefined')) { tmp.oldStyleDisplay = tmp.style.display; }
     tmp.style.display = (($getStyle(tmp, 'display') == 'none')?((tmp.oldStyleDisplay)?(tmp.oldStyleDisplay):('inline')):('none'));
     return true;   
    } 
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $toggleOpacity(millisec, node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{ 
try { 
     var _node = $(node || this); 
     var _fadeOpacity = function (node, millisec, opacityStart, opacityEnd) 
        {
         var speed = Math.round((millisec || 3000) / 1000);

         if(opacityStart > opacityEnd)        { for(var i = opacityStart, timer = 0; i >= opacityEnd; i--,timer++) { setTimeout("$setOpacity(" + i + ", '" + node.id + "');",(timer * speed)); } } 
         else { if(opacityStart < opacityEnd) { for(var i = opacityStart, timer = 0; i <= opacityEnd; i++,timer++) { setTimeout("$setOpacity(" + i + ", '" + node.id + "');",(timer * speed)); } } }
        };
     
     if(parseFloat(_node.style.opacity) == 0) { _fadeOpacity(_node, millisec, 0, 100); } 
     else                                     { _fadeOpacity(_node, millisec, 100, 0); }
     return true;
    }
catch(e) { if(myAJAX.debugMode) { alert('"$toggleOpacity" function ERROR: ' + e.message); } return false; }  
};
/*/////////////////////////////////////////////////////////////////////*/
function $toggleVisibility(node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var tmp = $(node || this); 
     tmp.style.visibility = (($getStyle(tmp, 'visibility') == 'hidden')?('visible'):('hidden')); 
     return true;   
    } 
catch(e) { if(myAJAX.debugMode) { alert('"$toggleVisibility" function ERROR: ' + e.message); } return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $updateHTML(html, node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var _node = $(node || this), _html = ((html.text)?(html.text):($stripScripts(html))), _js = ((html.javaScript)?(html.javaScript):($extractScripts(html)));
     if((_node.innerHTML.length != _html.length) || (_node.innerHTML != _html)) { _node.innerHTML = _html; }
     if(_js) { try { setTimeout(_js, 10); } catch(e) { } }
     return true;   
    } 
catch(e) { if(myAJAX.debugMode) { alert('"$updateHTML" function ERROR: ' + e.message); } return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $updateText(text, node)
/*///HTMLElement///////////////////////////////////////////////////////*/
{
try { 
     var _node = $(node || this), _nodeText = (typeof(_node.innerText) == 'undefined')?((typeof(_node.textContent) == 'undefined')?(''):(_node.textContent)):(_node.innerText), _text = ((text.text)?(text.text):($stripScripts(text))), _js = ((text.javaScript)?(text.javaScript):($extractScripts(text))); 
     if((_nodeText.length != _text.length) || (_nodeText != _text)) 
        { 
         if(typeof(_node.innerText) != 'undefined') { _node.innerText = _text; }
         else { if(typeof(_node.textContent) != 'undefined') { _node.textContent = _text; } }        
        }
     if(_js) { try { setTimeout(_js, 10); } catch(e) { } }
     return true;   
    } 
catch(e) { if(myAJAX.debugMode) { alert('"$updateText" function ERROR: ' + e.message); } return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $isIframeCapableBrowser()
/*///navigator/////////////////////////////////////////////////////////*/
{
try {
     var iframe = document.createElement('iframe');
     return (iframe.nodeName.toUpperCase() == 'IFRAME');
    } 
catch(e) { if(myAJAX.debugMode) { alert('"$isIframeCapableBrowser" function ERROR: ' + e.message); } return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $inheritFromObject(sourceObject, destinationObject)
/*///Object////////////////////////////////////////////////////////////*/
{
try
        { 
         var p, tmp = (typeof(destinationObject) == 'undefined')?(this):(destinationObject);
         if(!tmp) { return null; } 
         for(p in sourceObject) 
                { try {if(!tmp[p]) { tmp[p] = sourceObject[p]; }} catch(e) { } } 
         return tmp;
        }
catch(e) { if(myAJAX.debugMode) { alert('"$inheritFromObject" function ERROR: ' + e.message); } return null; }        
};
/*/////////////////////////////////////////////////////////////////////*/
function $isString(string)
/*///Object////////////////////////////////////////////////////////////*/
{
try {
     var tmp = string || this;
     
     switch(typeof(tmp))
        {
                case 'string': 
                        return true;
        
                case 'object':
                        return (tmp instanceof(String));
                        
                default:
                        return false;
        }
    }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $setProperty(propertyName, propertyValue, object)
/*///Object////////////////////////////////////////////////////////////*/
{
try
        { 
         var tmp = (typeof(object) == 'undefined')?(this):(object);
         tmp[propertyName] = propertyValue; 
         return tmp;
        }
catch(e) { if(myAJAX.debugMode) { alert('"$addProperty" function ERROR: ' + e.message); } return null; }        
};
/*/////////////////////////////////////////////////////////////////////*/
function $baseName(string) 
/*///String////////////////////////////////////////////////////////////*/
{
try {
     var _baseName = (string || this).match(/[\/|\\]([^\\\/]+)$/);
     return (typeof(_baseName[1]) == 'undefined')?(string):(_baseName[1]);
    }
catch(e) { return ''; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function $escapeHTML(string) 
/*///String////////////////////////////////////////////////////////////*/
{
try      { return document.createElement('div').appendChild(document.createTextNode((typeof(string) == 'undefined')?(this):(string))).parentNode.innerHTML; }
catch(e) { return ''; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function $extractScripts(string)
/*///String////////////////////////////////////////////////////////////*/
/* This function was made with the help of Sebastian VASILE */
{ 
try {
      for (var javaScriptCode = (((typeof(string) == 'undefined')?(this):(string)).match(/(<script.*?>)(.|\r|\n)*?(?=<\/script>)/gim)), repl = /<script.*>/gim, i = (javaScriptCode.length-1); i> -1; i--){ javaScriptCode[i] = javaScriptCode[i].replace(repl, ''); } 
      return javaScriptCode.join('');
     }
catch(e) { return ''; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $getKey(key, string)
/*///String////////////////////////////////////////////////////////////*/
/* This function it is a modified function from jBookmarker project, made by Tudor BARBU */
{
if(!(key)) { throw new Error("`key` parameter is not set."); }
try
        {
         var s = (typeof(string) == 'undefined')?(this):(string);
         if(s.indexOf( key ) == -1) { return false; }
         var re = new RegExp("(?=" + key + "=)[^&^#]+");
         s = s.match(re); 
         return $urlDecode(s[0].substring( key.length + 1, s[0].length ));
        }
catch(e) { return false; }        
};
/*/////////////////////////////////////////////////////////////////////*/
function $getKeys(string)
/*///String////////////////////////////////////////////////////////////*/
/* This function was made with the help of Sebastian VASILE */
{
try      { return ((typeof(string) == 'undefined')?(this):(string)).match(/[^&|?]{1}\w*(?==)/g); }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $replaceAll(searchFor, replaceWith, searchedString)
/*///String////////////////////////////////////////////////////////////*/
{
if(!(searchFor)) { throw new Error("$replaceAll: `searchFor` parameter is not set."); }
try
        {
	 var re  = new RegExp(searchFor,'g');
	 return ((typeof(searchedString) == 'undefined')?(this):(searchedString)).replace(re, replaceWith); 
        } 
catch(e) { return (searchedString || this); }
};
/*/////////////////////////////////////////////////////////////////////*/
function $randomHash()
/*///String////////////////////////////////////////////////////////////*/
{ return new Date().getTime() + new String(Math.random()).substr(2); };
/*/////////////////////////////////////////////////////////////////////*/
function $removeKey(key, string)
/*///String////////////////////////////////////////////////////////////*/
/* This function it is a modified function from jBookmarker project, made by Tudor BARBU */
{
if(!(key)) { throw new Error("`key` parameter is not set."); }
try 
        {
         var s = (typeof(string) == 'undefined')?(this):(string);
         if (s.indexOf( key ) == -1) { return false; }
         if((s.charAt(0) == '#') || (s.charAt(0) == '&')) { s = s.substr(1); }
         var re = new RegExp( "(?=" + key + "=)[^&]+" );
         s = s.replace( re, '' );
         if((s.charAt(0) == '#') || (s.charAt(0) == '&')) { s = s.substr(1); }
         if(s.charAt(s.length-1) == '&') { s = s.substr(0, s.length-1); }
         return true;
        }
catch(e) { return false; }        
};
/*/////////////////////////////////////////////////////////////////////*/
function $setKey(key, value, string)
/*///String////////////////////////////////////////////////////////////*/
/* This function it is a modified function from jBookmarker project, made by Tudor BARBU */
{
if(!(key)) { throw new Error("`key` parameter is not set."); }
try
        {
         value = (value)?($urlEncode(value).$replaceAll('=', '%3D').$replaceAll('&','%26')):('');
         var s = (typeof(string) == 'undefined')?(this):(string);
         if((s.charAt(0) == '#') || (s.charAt(0) == '&')) { s = s.substr(1); }
         if (s.indexOf( key + '=' ) != -1) 
                {
                 var re = new RegExp( "(?=" + key + "=)[^&]+" );
                 s = s.replace( re, key + '=' + value);
                }
         else { s = (s.length == 0)?(key + '=' + value):(s.concat( '&' + key + '=' + value)); }
         return s;
        } 
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $stripCRLF(string) 
/*///String////////////////////////////////////////////////////////////*/
{
try      { return ((typeof(string) == 'undefined')?(this):(string)).replace(/\r\n/g,'').replace(/\n/g,''); }
catch(e) { return false;}    
};
/*/////////////////////////////////////////////////////////////////////*/
function $stripScripts(string)
/*///String////////////////////////////////////////////////////////////*/
{ 
try { return (((typeof(string) == 'undefined')?(this):(string)).replace(/(<script.*?>)(.|\r|\n)*?(?=<\/script>)/gim,'')).replace(/<\/script>/gim,''); }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $stripTags(string) 
/*///String////////////////////////////////////////////////////////////*/
{
try      { return ((typeof(string) == 'undefined')?(this):(string)).replace(/(<([^>]+)>)/ig,''); }
catch(e) { return false;}    
};
/*/////////////////////////////////////////////////////////////////////*/
function $trim(string) 
/*///String////////////////////////////////////////////////////////////*/
{
try { return ((typeof(string) == 'undefined')?(this):(string)).replace(/^\s*(\S*(\s+\S+)*)\s*$/, "$1"); }
catch(e) { return false;}    
};
/*/////////////////////////////////////////////////////////////////////*/
function $unescapeHTML(string) 
/*///String////////////////////////////////////////////////////////////*/
{
try      {
          var div = document.createElement('div');
          div.innerHTML = ((typeof(string) == 'undefined')?(this):(string)); 
          return (typeof(div.innerText) == 'undefined')?((typeof(div.textContent) == 'undefined')?(false):(div.textContent)):(div.innerText);
         }
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/  
function $urlEncode(urlParameters)
/*///String////////////////////////////////////////////////////////////*/
{ 
try
        {
         for(var encodedURL = unescape((typeof(urlParameters) == 'undefined')?(this):(urlParameters)).split('&'), encodedURLLength = encodedURL.length, i = 0;i < encodedURLLength; i++) 
                {
                 encodedURL[i]=encodedURL[i].split('=');
                 for(j = 0;j < encodedURL[i].length;j++) { encodedURL[i][j] = escape(encodedURL[i][j]); }
                 encodedURL[i] = encodedURL[i].join('=');
                } 
         encodedURL = encodedURL.join('&');
         return encodedURL;      
        }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/  
function $urlDecode(urlParameters)
/*///String////////////////////////////////////////////////////////////*/
{ 
try
        { 
         var tmp = (typeof(urlParameters) == 'undefined')?(this):(urlParameters);
         return (tmp)?(escape(tmp).$replaceAll('%3D','=').$replaceAll('%253D','=').$replaceAll('%26','&').$replaceAll('%2526','&')):(tmp);
        }
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function $scroll(x, y)
/*///window////////////////////////////////////////////////////////////*/
/* This function it is a modified function, originally made by Tudor BARBU */
{
try {
     if((typeof(x) != 'undefined') && (typeof(y) != 'undefined'))
        { 
         window.scroll(parseInt(x), parseInt(y));
         return true;
        } 
     window.scroll(0, $(x).$getPosition().y);
     return true;
    }   
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function $getMousePosition(event)
/*///window////////////////////////////////////////////////////////////*/
{
try {
     event = event || window.event;
     return { x: event.clientX + document.body.scrollLeft + document.documentElement.scrollLeft || event.pageX + window.scrollX || 0, y: event.clientY + document.body.scrollTop  + document.documentElement.scrollTop || event.pageY + window.scrollY || 0 };
    }   
catch(e) { return null; }    
};
/*/////////////////////////////////////////////////////////////////////*/
if (!window.HTMLElement) 
        { 
         try
                {
                 document.___getElementById       = document.getElementById;
                 document.getElementById          = function(id)  { return $inheritFromObject(HTMLElement.prototype, document.___getElementById(id));  };
                 
         
                 document.___createElement        = document.createElement;
                 document.createElement           = function(tag) { return $inheritFromObject(HTMLElement.prototype, document.___createElement(tag));  };
         
                 document.___createTextNode       = document.createTextNode;
                 document.createTextNode          = function(text) { return $inheritFromObject(HTMLElement.prototype, document.___createTextNode(text));  };

                 document.___getElementsByName    = document.getElementsByName;
                 document.getElementsByName       = function(name) 
                        { 
                         var element, list = document.___getElementsByName(name);
          
                         for(element in list) { $inheritFromObject(HTMLElement.prototype, element); }
                         return list;
                        };
         
                 document.___getElementsByTagName = document.getElementsByTagName;
                 document.getElementsByTagName    = function(tagName) 
                        { 
                         var element, list = document.___getElementsByTagName(tagName);
          
                         for(element in list) { $inheritFromObject(HTMLElement.prototype, list[element]); }
                         return list;
                        };

                }
         catch(e) { }       
         var HTMLElement = function(){ }; 
         HTMLElement.prototype = new Object();
         HTMLElement.prototype.myAJAX = true;
        }
        
/*/////////////////////////////////////////////////////////////////////*/
Array.prototype.$inArray                   = $inArray;
Array.prototype.$indexOf                   = $indexOf;
/*/////////////////////////////////////////////////////////////////////*/
document.$createElement                    = $createElement;
/*/////////////////////////////////////////////////////////////////////*/
Function.prototype.$body                   = $body;
/*/////////////////////////////////////////////////////////////////////*/
HTMLElement.prototype.$captureEvent        = $captureEvent;
HTMLElement.prototype.$copyStyle           = $copyStyle;
HTMLElement.prototype.$getPosition         = $getPosition;
HTMLElement.prototype.$getStyle            = $getStyle;
HTMLElement.prototype.$hide                = $hide;
HTMLElement.prototype.$isForm              = $isForm;
HTMLElement.prototype.$isFormWithFileInput = $isFormWithFileInput;
HTMLElement.prototype.$object2QueryString  = $object2QueryString;
HTMLElement.prototype.$releaseEvent        = $releaseEvent;
HTMLElement.prototype.$remove              = $remove;
HTMLElement.prototype.$reset               = $reset;
HTMLElement.prototype.$setOpacity          = $setOpacity;
HTMLElement.prototype.$show                = $show;
HTMLElement.prototype.$toggleDisplay       = $toggleDisplay;
HTMLElement.prototype.$toggleOpacity       = $toggleOpacity;
HTMLElement.prototype.$toggleVisibility    = $toggleVisibility;
HTMLElement.prototype.$updateHTML          = $updateHTML;
HTMLElement.prototype.$updateText          = $updateText;
/*/////////////////////////////////////////////////////////////////////*/
navigator.$isIframeCapableBrowser          = $isIframeCapableBrowser;
/*/////////////////////////////////////////////////////////////////////*/
Object.prototype.$inheritFromObject        = $inheritFromObject;
Object.prototype.$isString                 = $isString;
Object.prototype.$setProperty              = $setProperty;
/*/////////////////////////////////////////////////////////////////////*/
String.prototype.$                         = $;
String.prototype.$baseName                 = $baseName;
String.prototype.$escapeHTML               = $escapeHTML;
String.prototype.$extractScripts           = $extractScripts;
String.prototype.$getKey                   = $getKey;
String.prototype.$getKeys                  = $getKeys;
String.prototype.$replaceAll               = $replaceAll;
String.prototype.$randomHash               = $randomHash;
String.prototype.$removeKey                = $removeKey;
String.prototype.$setKey                   = $setKey;
String.prototype.$stripCRLF                = $stripCRLF;
String.prototype.$stripScripts             = $stripScripts;
String.prototype.$stripTags                = $stripTags;
String.prototype.$trim                     = $trim;
String.prototype.$unescapeHTML             = $unescapeHTML;
String.prototype.$urlEncode                = $urlEncode;
String.prototype.$urlDecode                = $urlDecode;
/*/////////////////////////////////////////////////////////////////////*/
$inheritFromObject(HTMLElement.prototype, window);
$inheritFromObject(HTMLElement.prototype, window.document);
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/  
/*/////////////////////////////////////////////////////////////////////*/  
function myAJAX(url /* = null */, urlParameters /* = null */, method /* = 'GET' */, refreshInterval /* = 0 */, onStartFunction /* = null */, onProgressFunction /* = null */, onReadyFunction /* = null */, onErrorFunction /* = null */, InnerHtmlId /* = null */, ajaxMethod /* = undefined */, forceNoCache /* = true*/, enableMultiplyingFileInputs /* = false */, enableStyleEffects /* = false */)
/*///myAJAX////////////////////////////////////////////////////////////*/
/* 

Object myAJAX 
/////////////////////////////////////////////////////////////////////////

   PARAMETERS
-------------------------------------------------------------------------
url                         = page's URL
urlParameters               = page's parameters (can be a string or an object pointing to a form or to a single form element)
method                      = 'HEAD', 'GET' or 'POST' with a defaul value of 'GET'
refreshInterval             = sets the execution of AJAX request periodically for values not zero; it is expressed in msec.
onStartFunction             = function called before the request
onProgressFunction          = function called on interactive mode
onReadyFunction             = function called when server's response is myAJAX.constants.HTTP.serverStatus.OK 
onErrorFunction             = function called when server's response is other than myAJAX.constants.HTTP.serverStatus.OK 
InnerHtmlId                 = is the ID of an html element who will be populated with the text response
ajaxMethod                  = `XMLHttp` or `iFrame`, with a default value of `XMLHttp`
forceNoCache                = force to not use broser's cache for AJAX requests (default is true)
enableMultiplyingFileInputs = enable multiplying file inputs on `change` event for multiple file uploads.
enableStyleEffects          = enable style effects applied on form's elements (default is false)

OR

url                         = an object
   
   PROPERTIES
-------------------------------------------------------------------------
ID                          = returns current object's ID
(the rest of the object's properties are the same with `PARAMETERS`)

   METHODS
-------------------------------------------------------------------------
namedInstance(instanceName) = create and/or just return specified instance name
sendRequest()               = performs AJAX request
abortRequest()              = aborts AJAX request

/////////////////////////////////////////////////////////////////////////

*/ 
{
if (!(this instanceof (myAJAX))) { return new myAJAX(url, urlParameters, method, refreshInterval, onProgressFunction, onReadyFunction, onErrorFunction, InnerHtmlId, ajaxMethod, forceNoCache, enableMultiplyingFileInputs, enableStyleEffects); }

var thisObject                     = this;
var XMLHttpObject;
var intervalID                     = 0;
var objectID                       = 'myAJAX' + myAJAX._vars._currentObjectID++;
var _parameterObject               = (((arguments.length == 1) && (url != null) && (typeof(url) == 'object') && (!(url instanceof (String))))?(url):(false));
var _isIframeCapableBrowser        = $isIframeCapableBrowser();

this.url                           = (_parameterObject)?(_parameterObject.url):(url);
this.urlParameters                 = (_parameterObject)?(_parameterObject.urlParameters):(urlParameters);
this.method                        = (_parameterObject)?(_parameterObject.method):(method || myAJAX.constants.defaultValues.method);
this.refreshInterval               = (_parameterObject)?(_parameterObject.refreshInterval):(refreshInterval);
this.onStartFunction               = (_parameterObject)?(_parameterObject.onStartFunction):(onStartFunction);
this.onProgressFunction            = (_parameterObject)?(_parameterObject.onProgressFunction):(onProgressFunction);
this.onReadyFunction               = (_parameterObject)?(_parameterObject.onReadyFunction):(onReadyFunction);
this.onErrorFunction               = (_parameterObject)?(_parameterObject.onErrorFunction):(onErrorFunction);
this.InnerHtmlId                   = (_parameterObject)?(_parameterObject.InnerHtmlId):(InnerHtmlId);
this.ajaxMethod                    = (_parameterObject)?(_parameterObject.ajaxMethod):(ajaxMethod || myAJAX.constants.ajaxMethod.auto);
this.forceNoCache                  = (_parameterObject)?(_parameterObject.forceNoCache):(forceNoCache || myAJAX.constants.defaultValues.forceNoCache);
this.enableMultiplyingFileInputs   = (_parameterObject)?(_parameterObject.enableMultiplyingFileInputs):(enableMultiplyingFileInputs || myAJAX.constants.defaultValues.enableMultiplyingFileInputs);
this.enableStyleEffects            = (_parameterObject)?(_parameterObject.enableStyleEffects):(enableStyleEffects);

_parameterObject                   = null;
/*/////////////////////////////////////////////////////////////////////*/
try      { 
          this.__defineGetter__('ID',      function() { return objectID; }); 
          this.__defineGetter__('version', function() { return myAJAX.version; }); 
         } 
catch(e) { 
          this.ID      = new String(objectID); 
          this.version = new String(myAJAX.version);
         }
/*/////////////////////////////////////////////////////////////////////*/
if(this.enableMultiplyingFileInputs && (!(/Netscape\/*[0-7]/i.test(navigator.userAgent))))
        { 
         var f = function() 
         { 
          try 
                {
                 var form = $(f.form); 
                 if(!$isFormWithFileInput(form)) { return false; }
                 var fileInputs = []; 

                 try 
                        {
                         for(var i = 0, formElements = form.elements.length, currentElement = form.elements[i]; i < formElements; currentElement = form.elements[++i]) 
                                { 
                                 try { if((currentElement.nodeName.toUpperCase() == 'INPUT') && (currentElement.getAttribute('type').toUpperCase() == 'FILE')) { fileInputs.push(currentElement); } }   
                                 catch(e) { }
                                }
                        }
                 catch(e) { fileInputs = []; }    

          
                 for(var i = (fileInputs.length - 1), fileInput = $(fileInputs[i]); i > -1; fileInput = $(fileInputs[--i]))
                        { fileInput.$captureEvent('change', myAJAX.cloneFileInputOnChange); }
          
                 form.$captureEvent('reset', myAJAX.resetClonedFileInputsOnFormReset);
                }
          catch(e) { if(myAJAX.debugMode) { alert('"f" function ERROR: ' + e.message); } }      
         };

         f.form = this.urlParameters;
         window.$captureEvent('load', f);
         
        }
/*/////////////////////////////////////////////////////////////////////*/
this.sendRequest        = function()
/*///myAJAX////////////////////////////////////////////////////////////*/
{
try 
        {
        _stopAutoRefresh();
         var refreshInterval                              = (isNaN(thisObject.refreshInterval))?(myAJAX.constants.defaultValues.refreshInterval):(parseInt(thisObject.refreshInterval));
         var urlParametersIsReferencingAFormWithFileInput = $isFormWithFileInput(thisObject.urlParameters);
         var urlParameters                                =  $object2QueryString(thisObject.urlParameters);
         thisObject.ajaxMethod                            = (typeof(thisObject.ajaxMethod) == 'undefined')?(myAJAX.constants.ajaxMethod.auto):((myAJAX.constants.ajaxMethod.regExpTest.test(thisObject.ajaxMethod))?(thisObject.ajaxMethod.toUpperCase()):(myAJAX.constants.ajaxMethod.auto));
         thisObject.enableStyleEffects                    = thisObject.enableStyleEffects && (!(/Netscape\/*[0-6]/i.test(navigator.userAgent)));   
         if(thisObject.ajaxMethod == myAJAX.constants.ajaxMethod.auto)
                { 
                 if(urlParametersIsReferencingAFormWithFileInput)
                        { 
                         XMLHttpObject = null;
                         if(_isIframeCapableBrowser) { thisObject.ajaxMethod = myAJAX.constants.ajaxMethod.iFrame; }
                         else { throw new Error(myAJAX.constants.errorMessages.iframeElementNotSupported); }
                        }
                 else
                        {
                         try      { 
                                   XMLHttpObject = new myAJAX.XMLHttp(); 
                                   thisObject.ajaxMethod = myAJAX.constants.ajaxMethod.XMLHttp;
                                  }
                         catch(e) { 
                                   XMLHttpObject = null; 
                                   if(_isIframeCapableBrowser) { thisObject.ajaxMethod = myAJAX.constants.ajaxMethod.iFrame; }
                                   else { throw new Error(myAJAX.constants.errorMessages.iframeElementNotSupported); }
                                  }
                        } 
                }
                
         if(thisObject.ajaxMethod == myAJAX.constants.ajaxMethod.XMLHttp) 
                { /*use `XMLHttp` based method */ 
                 if(thisObject.enableStyleEffects && $isForm(thisObject.urlParameters)) { myAJAX.formElements2span(urlParameters, objectID, true); }
                 myAJAX.ajax(thisObject.url, urlParameters, thisObject.method, thisObject.onStartFunction, thisObject.onProgressFunction, thisObject.onReadyFunction, thisObject.onErrorFunction, thisObject.InnerHtmlId, thisObject.forceNoCache, XMLHttpObject);
                 if(refreshInterval && (refreshInterval > 0)) 
                        {
                         myAJAX._vars._XMLHttpObjects[objectID] = XMLHttpObject;
                         intervalID = setInterval('myAJAX.ajax("' + (($isString(thisObject.url))?(thisObject.url):('')) + '", "' + urlParameters + '", "' + (($isString(thisObject.method))?(thisObject.method):('')) + '", ' + thisObject.onStartFunction + ', ' + thisObject.onProgressFunction + ', ' + thisObject.onReadyFunction + ', ' + thisObject.onErrorFunction + ', "' + (($isString(thisObject.InnerHtmlId))?(thisObject.InnerHtmlId):('')) + '", ' + thisObject.forceNoCache +', "' + objectID + '");', refreshInterval);
                        }
                 return true;       
                }
         else
                { /*use `iFrame` based method */ 
                 XMLHttpObject = null;
                 myAJAX.iFrame(objectID, thisObject.url, thisObject.urlParameters, thisObject.method, thisObject.onStartFunction, thisObject.onReadyFunction, thisObject.onErrorFunction, thisObject.InnerHtmlId, thisObject.forceNoCache, thisObject.enableStyleEffects, urlParametersIsReferencingAFormWithFileInput);
                 if(refreshInterval && (refreshInterval > 0) && (!urlParametersIsReferencingAFormWithFileInput))
                        { 
                         myAJAX._vars._urlParameters[objectID] = thisObject.urlParameters;
                         intervalID = setInterval('myAJAX.iFrame("' + objectID + '", "' + (($isString(thisObject.url))?(thisObject.url):('')) + '", null, "' + (($isString(thisObject.method))?(thisObject.method):('')) + '", ' + thisObject.onStartFunction + ', ' + thisObject.onReadyFunction + ', ' + thisObject.onErrorFunction + ', "' + (($isString(thisObject.InnerHtmlId))?(thisObject.InnerHtmlId):('')) + '", ' + thisObject.forceNoCache +', ' + thisObject.enableStyleEffects + ', ' + urlParametersIsReferencingAFormWithFileInput + ' );', refreshInterval); 
                        }
                }
        }
catch(e)
        { 
         thisObject.abortRequest();
         if(myAJAX.debugMode) { alert('"sendRequest" function ERROR: '+e.message); }
         throw e;       
        }
};
/*/////////////////////////////////////////////////////////////////////*/
this.abortRequest       = function()
/*///myAJAX////////////////////////////////////////////////////////////*/ 
{
_stopAutoRefresh();
try { 
     if(XMLHttpObject != null)
        {
         var HTTPstate            = myAJAX.constants.HTTP.readyState;
         var _ajaxRequestObject   = XMLHttpObject.requestObject;

         if(_ajaxRequestObject && (_ajaxRequestObject.readyState != HTTPstate.Uninitialized) && (_ajaxRequestObject.readyState != HTTPstate.Complete)) { _ajaxRequestObject.abort(); }   
        }
     else
        { 
         var newiFrameID = objectID + 'iframe';
         var iFrame      = newiFrameID.$(); 
         myAJAX._methods._iFrameBugFix4IEDOM(newiFrameID);
         var doc = (iFrame.contentDocument)?(iFrame.contentDocument):((iFrame.contentWindow)?(iFrame.contentWindow.document):(self.frames[newiFrameID].document));         
         doc.location.href = 'about:blank';
        }
     return true;
    } 
catch(e) { return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
function _stopAutoRefresh()
/*///myAJAX////////////////////////////////////////////////////////////*/
{
try { if(intervalID) { clearInterval(intervalID); } }
catch(e) {}
try { 
     if(myAJAX._vars._urlParameters[objectID])  { myAJAX._vars._urlParameters[objectID]  = null; }
     if(myAJAX._vars._XMLHttpObjects[objectID]) { myAJAX._vars._XMLHttpObjects[objectID] = null; } 
    }
catch(e) { }
};
/*/////////////////////////////////////////////////////////////////////*/
}
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/ 
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.version = '6.1.0';
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.debugMode = false;
/*/////////////////////////////////////////////////////////////////////*/
myAJAX._vars  = { _currentObjectID: 1, _urlParameters: {}, _XMLHttpObjects: {}, _instances: {}, _W3CDOM: (document.createElement && document.appendChild && document.getElementsByTagName && document.getElementById), _applyInheritance: (HTMLElement.prototype.myAJAX === true) };
/*/////////////////////////////////////////////////////////////////////*/
myAJAX._methods = { _iFrameBugFix4IEDOM: function(frameID) {try { if(self.frames[frameID].name != frameID) { /* *** IMPORTANT: This is a BUG FIX for Internet Explorer when the `iframe` is created with standard DOM methods not with the IE's specific method of `createElement` *** */ self.frames[frameID].name = frameID; } } catch(e) { }}, _eventCallBackManager: function(callBackFunction) { if (!(this instanceof (myAJAX._methods._eventCallBackManager))) { return new myAJAX._methods._eventCallBackManager(callBackFunction).eventCallBackManager; } var callBackFunctions = []; if(callBackFunction) { callBackFunctions.push(callBackFunction); } this.eventCallBackManager = function(event){ event = event || window.event; for(var i = 0, callBackFunctionsLength = callBackFunctions.length;i < callBackFunctionsLength; ++i) { try { if(typeof(callBackFunctions[i]) == 'function') { callBackFunctions[i](event); } } catch(e) { } } }; this.eventCallBackManager.myAJAX         = true; this.eventCallBackManager.addCallBack    = function(callback) { callBackFunctions.push(callback); return true; }; this.eventCallBackManager.removeCallBack = function(callback) { try { callBackFunctions[callBackFunctions.$indexOf(callback)] = null; return true; } catch(e) { return false; } }; } };
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.cloneFileInputOnChange = function(fileInputElement)
/*///myAJAX////////////////////////////////////////////////////////////*/
{
try 
        { 
         var input = ((typeof(fileInputElement.srcElement) == 'object')?(fileInputElement.srcElement):((typeof(fileInputElement.target) == 'object')?(fileInputElement.target):(fileInputElement)));
         
         if(input.value.length==0) { return false; } 
         var name = input.getAttribute('name');
         if(!/.+[[]{1}\]$/.test(name)) { name += '[]'; input.setAttribute('name', name); }
         var nextFileInput = input.nextSibling.nextSibling; 
         if(nextFileInput && (nextFileInput.nodeName.toUpperCase() == 'INPUT') && (nextFileInput.getAttribute('name') == name) && (nextFileInput.getAttribute('type').toUpperCase() == 'FILE')) { return false; }
         var br            = input.parentNode.insertBefore(document.createElement('br'), input.nextSibling);
         var clonedNode    = input.cloneNode(true);
         clonedNode.setAttribute('value', '');
         if(clonedNode.value) { clonedNode.value = ''; }
         var id            = clonedNode.getAttribute('id');
         if(id) { clonedNode.setAttribute('id', id + '_');  }
         clonedNode.setAttribute('myAJAXclone', 'true');
         if(clonedNode.name != name) { clonedNode.name = name; }
         if(clonedNode.getAttribute('value') || clonedNode.value) { clonedNode = null; }
         else { 
               clonedNode = $(input.parentNode.insertBefore(clonedNode, br.nextSibling)); 
               clonedNode.$captureEvent('change', myAJAX.cloneFileInputOnChange);
              }
        }
catch(e) { if(myAJAX.debugMode) { alert('"$cloneFileInputOnChange" function ERROR: '+e.message); } return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.resetClonedFileInputsOnFormReset = function(form)
/*///myAJAX////////////////////////////////////////////////////////////*/
{ 
try
        { 
         var nextSibling, _form = $isForm(form)?($(form)):(((typeof(form.srcElement) == 'object')?(form.srcElement):((typeof(form.target) == 'object')?(form.target):(null))));
       
         for(var i = (_form.elements.length - 1); i > -1; --i)
                { 
                 formElement = _form.elements[i]; 
                 if(formElement.getAttribute('myAJAXclone') == 'true')
                        {
                         try
                                {
                                 nextSibling = formElement.nextSibling;
                                 if(nextSibling && (nextSibling.nodeName.toUpperCase() == 'BR')) { $remove(nextSibling); }
                                 var spanID = formElement.getAttribute('spanID'); 
                                 if(spanID) 
                                        { 
                                         var span = spanID.$();
                                         if(span) 
                                                { 
                                                 $remove(span.nextSibling);
                                                 $remove(span); 
                                                }
                                        }
                                 $remove(formElement);
                                }
                         catch(e) { if(myAJAX.debugMode) { alert('"resetClonedFileInputsOnFormReset" function ERROR: '+e.message); } }       
                        }
                }
         return true;
        }
catch(e) { if(myAJAX.debugMode) { alert('"resetClonedFileInputsOnFormReset" function ERROR: ' + e.message); } return false; }

};
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.formElements2span = function (form, objectID, withStyle)
/*///myAJAX////////////////////////////////////////////////////////////*/
{
try {
     form = $(form); 
     var divID                          = objectID + 'div';
     var formElementIDprefix            = objectID + 'element-';
     var spanIDprefix                   = objectID + 'span-';
     var spanFormElementIDAttributeName = objectID + 'elementID'; 
     var s, sText, sID, currentFileInput;
     var _hiddenDIV = function (divID) 
     { /* ----------------------- */
      try { 
            var div = divID.$(); 
            if(div) { return div; } 
            return document.$createElement('div', { 'id': divID, 'name': divID, 'style': { 'display': 'none', 'width': '0px', 'height': '0px', 'border': 'none' } }, form);            
          } 
      catch(e) { if(myAJAX.debugMode) { alert('"formElements2span" function ERROR: '+e.message); } return null; } 
     };
     var _formElements = function (form, divID) 
     { /* ----------------------- */
      try { 
            var elementNodeName, elementType, arrFormElements = [];
            
            if((!form) || (form.nodeName.toUpperCase() != 'FORM')) { return false; } 
            for(var i = 0, formElements = form.elements.length, currentElement = form.elements[i]; i < formElements; currentElement = form.elements[++i]) 
                { 
                 elementNodeName = currentElement.nodeName.toUpperCase(); 
                 
                 if(currentElement.parentNode.getAttribute('id') != divID)
                        { 
                         switch(elementNodeName)
                                {
                                        case 'INPUT':
                                                try      { elementType = currentElement.getAttribute('type').toUpperCase(); }
                                                catch(e) { elementType = 'TEXT'; }
                                                if((elementType == 'TEXT') || (elementType == 'PASSWORD') || (elementType == 'FILE'))
                                                        {
                                                         if(currentElement.getAttribute('id')); 
                                                         else { currentElement.setAttribute('id',formElementIDprefix + $randomHash()); } 
                                                         arrFormElements.push(currentElement); 
                                                        }
                                                break;         
                                
                                        case 'SELECT': 
                                        case 'TEXTAREA':
                                                if(currentElement.getAttribute('id')); 
                                                else { currentElement.setAttribute('id',formElementIDprefix + $randomHash()); } 
                                                arrFormElements.push(currentElement); 
                                                break;
                                }
                         
                        } 
                } 
            return arrFormElements; 
          }
      catch(e) { if(myAJAX.debugMode) { alert('"_formElements" function ERROR: '+e.message); } return false; } 
     };    
     var _formElement2span = function(formElement)  
     { /* ----------------------- */
      try { 
            var s, formElementType;
            var sID = spanIDprefix + $randomHash();

            try      { formElementType = formElement.getAttribute('type').toUpperCase(); }
            catch(e) { formElementType = (formElement.nodeName.toUpperCase() == 'INPUT')?('TEXT'):(''); }
            var spanText   = (formElementType == 'PASSWORD')?(formElement.value.replace(/./g,'*')):(formElement.getAttribute('value') || formElement.value); 
            s = document.$createElement('span', { 'id': sID }.$setProperty(spanFormElementIDAttributeName, formElement.getAttribute('id')));
            s.appendChild(document.createTextNode(spanText));
            if(spanText.length > 0) { if(withStyle == true) { $copyStyle(s, formElement); } }
            else { s.$toggleDisplay();  }
            formElement.setAttribute('spanID',sID);
            s.$captureEvent('click', function(event) { try { var span = ((typeof(event.srcElement) == 'object')?(event.srcElement):((typeof(event.target) == 'object')?(event.target):(null))); var formElement = $(span.getAttribute(spanFormElementIDAttributeName)); span.parentNode.replaceChild(formElement, span);  return true; } catch(e) { try { /* *** IMPORTANT: This is a BUG FIX for Netscape Navigator's `removeChild` *** */ span.parentNode.insertBefore(formElement, span); span.$remove(); return true; } catch(e) { return false; } } }); 
            var formElementParentNode = formElement.parentNode;
            formElementParentNode.replaceChild(s, formElement);
            hiddenDIV.appendChild(formElement); 
            return true; 
          } 
      catch(e) { if(myAJAX.debugMode) { alert('"_formElement2span" function ERROR: '+e.message); } return false; }
     };
     
     var formElements = _formElements(form, divID);
     var hiddenDIV    = _hiddenDIV(divID);
     withStyle        = new Boolean(withStyle);

     if(hiddenDIV)
        {
         for(var i = 0, formElementsLength = formElements.length, currentFileInput = $(formElements[i]); i < formElementsLength; currentFileInput = $(formElements[++i]) )  { _formElement2span(currentFileInput); }
         return true;
        }
      else return false;  
    }
catch(e) { if(myAJAX.debugMode) { alert('"formElements2span" function ERROR: '+e.message); } return false; }
};
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.namedInstance = function(instanceName, url /* = null */, urlParameters /* = null */, method /* = 'GET' */, refreshInterval /* = 0 */, onStartFunction /* = null */, onProgressFunction /* = null */, onReadyFunction /* = null */, onErrorFunction /* = null */, InnerHtmlId /* = null*/, ajaxMethod /* = undefined */, forceNoCache /* = true*/, enableMultiplyingFileInputs /* = false */, enableStyleEffects /* = false */) 
/*///myAJAX////////////////////////////////////////////////////////////*/ 
{
if(!(instanceName)) {throw new Error(myAJAX.constants.errorMessages.InstanceNameUndefined); }
if(typeof(myAJAX._vars._instances[instanceName]) == 'undefined') { myAJAX._vars._instances[instanceName] = new myAJAX(url, urlParameters, method, refreshInterval, onProgressFunction, onReadyFunction, onErrorFunction, InnerHtmlId, ajaxMethod, forceNoCache, enableMultiplyingFileInputs, enableStyleEffects); } 
return myAJAX._vars._instances[instanceName];
};
/*/////////////////////////////////////////////////////////////////////*/ 
myAJAX.ajax = function (url, urlParameters/* = null */, method /* = 'GET' */, onStartFunction /* = null */, onProgressFunction /* = null */, onReadyFunction /* = null */, onErrorFunction /* = null */, InnerHtmlId /* = null */, forceNoCache /* = true */, XMLHttpObject /* = null*/)
/*///myAJAX////////////////////////////////////////////////////////////*/ 
{
if(!(url)) { throw new Error(myAJAX.constants.errorMessages.UrlUndefined); }

var HTTPreadyState       = myAJAX.constants.HTTP.readyState, ajaxRequestObject;
try { ajaxRequestObject  = (XMLHttpObject)?((XMLHttpObject instanceof (myAJAX.XMLHttp))?(XMLHttpObject.requestObject):((myAJAX._vars._XMLHttpObjects[XMLHttpObject])?((myAJAX._vars._XMLHttpObjects[XMLHttpObject] instanceof (myAJAX.XMLHttp))?(myAJAX._vars._XMLHttpObjects[XMLHttpObject].requestObject):(new myAJAX.XMLHttp().requestObject)):(new myAJAX.XMLHttp().requestObject))):((ajax.XMLHttpObject)?((ajax.XMLHttpObject instanceof (myAJAX.XMLHttp))?(ajax.XMLHttpObject.requestObject):(new myAJAX.XMLHttp().requestObject)):(new myAJAX.XMLHttp().requestObject)); } catch(e) { throw new Error(myAJAX.constants.errorMessages.NoAJAX); }
try { if((ajaxRequestObject.readyState != HTTPreadyState.Uninitialized) && (ajaxRequestObject.readyState != HTTPreadyState.Complete)) { return false; } } catch(e) { throw new Error(myAJAX.constants.errorMessages.unexpectedError); }
var HTTPserverStatus     = myAJAX.constants.HTTP.serverStatus, regExpTestMethod = new RegExp ('^get$|^post$|^head$','i'), regExpTestXML = myAJAX.constants.XML.regExpTestXML, random = "myAJAX=" + $randomHash();

urlParameters 	         = (urlParameters)?($urlEncode($object2QueryString(urlParameters))):(null);
method		         = new String((method)?((regExpTestMethod.test(method))?(method):(myAJAX.constants.defaultValues.method)):(myAJAX.constants.defaultValues.method)).toUpperCase();
onStartFunction          = (onStartFunction)?((typeof(onStartFunction) == 'function')?(onStartFunction):(null)):(null);
onProgressFunction       = (onProgressFunction)?((typeof(onProgressFunction) == 'function')?(onProgressFunction):(null)):(null);
onReadyFunction	         = (onReadyFunction)?((typeof(onReadyFunction) == 'function')?(onReadyFunction):(null)):(null);
onErrorFunction	         = (onErrorFunction)?((typeof(onErrorFunction) == 'function')?(onErrorFunction):(null)):(null);
InnerHtmlId	         = ($isString(InnerHtmlId))?(InnerHtmlId):(null);
forceNoCache	         = (forceNoCache)?(new Boolean(forceNoCache)):(myAJAX.constants.defaultValues.forceNoCache);

try { if(onStartFunction) { onStartFunction(); } } catch(e) { }
try { 

     switch(method)
                {
                        case "GET":    if(urlParameters) { ajaxRequestObject.open(method, url + "?" + urlParameters + ((forceNoCache==true)?('&' + random):('')), true); }
                                        else             { ajaxRequestObject.open(method, url + ((forceNoCache==true)?('?' + random):('')), true); } 
                                        break;
                                         
                        case "POST":
                        case "HEAD":    
                                        ajaxRequestObject.open(method, url + ((forceNoCache==true)?('?' + random):('')),true);
                                        break;
                }
     ajaxRequestObject.setRequestHeader("Content-Type",     "application/x-www-form-urlencoded"); 
     ajaxRequestObject.setRequestHeader("X-Requested-With", "myAJAX " + myAJAX.version); 

     if((onProgressFunction) || (onReadyFunction) || (onErrorFunction) || (InnerHtmlId)) 
        { 
         ajaxRequestObject.onreadystatechange = function() 
                {
                 if((ajaxRequestObject.readyState == HTTPreadyState.Interactive) && (onProgressFunction)) 
                        {
                         try       { var myContentLength = ajaxRequestObject.getResponseHeader("Content-Length"); }
                         catch (e) { myContentLength = 0; }
                         
                         try       { var myMsg = new String(ajaxRequestObject.responseText); }
                         catch (e) { myMsg = ''; }
                         
                         onProgressFunction(myMsg,new Number(myContentLength));
                        } 

                 if (ajaxRequestObject.readyState == HTTPreadyState.Complete) 
                        { 
                         if((ajaxRequestObject.status == HTTPserverStatus.OK) || (ajaxRequestObject.status == HTTPserverStatus.FileOK))
                                { 
                                
                                 if((regExpTestXML.test(ajaxRequestObject.getResponseHeader("Content-type"))) || (method == "HEAD"));
                                 else { /* execute javascript response if response isn't XML  */ var response = { text: $stripScripts(ajaxRequestObject.responseText), javaScript: $extractScripts(ajaxRequestObject.responseText) }; }
                                 
                                 if((InnerHtmlId) && (typeof(response) != 'undefined') && (typeof(response.text) != 'undefined')) { $updateHTML(response.text, InnerHtmlId); }
                                 
                                 if((typeof(response) != 'undefined') && (typeof(response.javaScript) != 'undefined')) { try { eval(response.javaScript); } catch(e) { } }                                                
                                        
                                 if(onReadyFunction) 
                                        { 
                                         if((ajaxRequestObject.responseXML) || (regExpTestXML.test(ajaxRequestObject.getResponseHeader("Content-type"))) || (method == "HEAD"));
                                         else { if(typeof(response) == 'undefined') { var response = { text: $stripScripts(ajaxRequestObject.responseText), javaScript: $extractScripts(ajaxRequestObject.responseText) }; } }
                                         try { onReadyFunction((method == "HEAD")?((new String(ajaxRequestObject.getAllResponseHeaders())).split("\r\n")):(((ajaxRequestObject.responseXML) || (regExpTestXML.test(ajaxRequestObject.getResponseHeader("Content-type"))))?(new myAJAX.XML(ajaxRequestObject.responseXML)):(new String(response.text))));  } 
                                         catch(e) { }
                                        }
                                }
                          else { if(onErrorFunction)  { onErrorFunction(new String(ajaxRequestObject.statusText),new Number(ajaxRequestObject.status)); } }
                          
                        } 
                }; 
        }
     else ajaxRequestObject.onreadystatechange = null;
     
     switch(method)
       {
                case "GET":     ajaxRequestObject.send(null);
                                break;
                                         
                case "POST":
                case "HEAD":    ajaxRequestObject.send(urlParameters);
                                break;
       }
     return ajaxRequestObject;
    } 
catch(e) { if(myAJAX.debugMode) { alert('"ajax" function ERROR: '+e.message); } throw e; }  
};
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.iFrame = function (objectID, url, urlParameters, method /* = 'GET' */, onStartFunction /* = null */, onReadyFunction /* = null */, onErrorFunction /* = null */, InnerHtmlId /* = null */, forceNoCache /* = true */, enableStyleEffects /* = false */, urlParametersIsReferencingAFormWithFileInput)
/*///myAJAX////////////////////////////////////////////////////////////*/ 
{ 
var regExpTestXML = myAJAX.constants.XML.regExpTestXML;
forceNoCache = (forceNoCache)?(new Boolean(forceNoCache)):(myAJAX.constants.defaultValues.forceNoCache);
InnerHtmlId  = ($isString(InnerHtmlId))?(InnerHtmlId):('');
/*/////////////////////////////////////////////////////////////////////*/
var _addIframe                     = function(objectID, onReadyFunction, onErrorFunction, InnerHtmlId)
/*///myAJAX (iFrame)///////////////////////////////////////////////////*/
{ /* ----------------------- */
try { 
     var newiFrameID = objectID + 'iframe'; 
     var iFrame      = newiFrameID.$();
     var _IExml      = function(doc)
     { /* ----------------------- */
      try { 
           if(doc && doc.body && doc.body.innerText)
                {
                 var xml        = doc.body.innerText.replace(/\r\n- /g,'');
                 var xmlElement = document.createElement('xml');
            
                 xmlElement.innerText = xml;
                 document.body.appendChild(xmlElement);
                 var xmlDoc = xmlElement.XMLDocument;
                 document.body.removeChild(xmlElement);
                 return (xmlDoc.documentElement)?(xmlDoc):(null);
                }
           return false;     
          }
      catch(e) { return false; } 
     };
     
     if(iFrame){ $remove(iFrame); }
     iFrame = document.$createElement('iframe', { 'id': newiFrameID, 'name': newiFrameID, 'src': 'about:blank', 'style': { 'display': 'none', 'width': '0px', 'height': '0px', 'border': 'none' }, 'myAJAXInnerHtmlId': InnerHtmlId }, document.body);
     iFrame.$captureEvent('load',  function(event)
        { 
         var _iFrame      = ((typeof(event.srcElement) == 'object')?(event.srcElement):((typeof(event.currentTarget) == 'object')?(event.currentTarget):(iFrame)));
         var _doc         = (_iFrame.contentDocument)?(_iFrame.contentDocument):((_iFrame.contentWindow)?(_iFrame.contentWindow.document):(self.frames[newiFrameID].document));                  

         try      { if(_doc.location.href    == 'about:blank') { return true; } }
         catch(e) { if(_iFrame.location.href == 'about:blank') { return true; } }

         var _InnerHtmlId;
         try      { _InnerHtmlId = _iFrame.getAttribute('myAJAXInnerHtmlId'); }
         catch(e) { _InnerHtmlId = InnerHtmlId; }
                 
         var response = _IExml(_doc); 
         if(response) { response = new myAJAX.XML(response); } 
         else         { response = (response == null)?($stripScripts(_doc.body.innerHTML)):((regExpTestXML.test(_doc.contentType) || (typeof(_doc.body) == 'undefined'))?(new myAJAX.XML(_doc)):($stripScripts(_doc.body.innerHTML))); }
                 
         if(!(response instanceof myAJAX.XML))
                { if(_InnerHtmlId) { $updateHTML(response, _InnerHtmlId); } } 
                
         if(typeof(onReadyFunction) == 'function') 
                {
                 try { onReadyFunction(response); }
                 catch(e) { }
                } 
         
         return true;
        }); 

     if(typeof(onErrorFunction) == 'function') { iFrame.$captureEvent('error', onErrorFunction); }  
     
     myAJAX._methods._iFrameBugFix4IEDOM(newiFrameID);
     return newiFrameID;
    }
catch(e) { if(myAJAX.debugMode) { alert('"_addIframe" function ERROR: '+e.message); } return false; }     
};
/*/////////////////////////////////////////////////////////////////////*/
var _addForm                       = function(objectID, url, urlParameters, method, onStartFunction, forceNoCache)
/*///myAJAX (iFrame)///////////////////////////////////////////////////*/
{ /* ----------------------- */
try {
     var formElements; 
     var newFormID            = objectID + 'form';
     var iFrameID             = objectID + 'iframe';
     var newForm              = newFormID.$();
     var regExpTestMethod     = new RegExp ('^get$|^post$','i');
     
     urlParameters     = $trim((urlParameters)?(urlParameters):(''));
     if(forceNoCache) { urlParameters = urlParameters.$setKey('myAJAX', $randomHash()); }
     urlParameters     = ((urlParameters[0] == '?')?(''):('?')) + urlParameters;
     urlParametersKeys = $getKeys(urlParameters); 
     method	       = new String((method)?((regExpTestMethod.test(method))?(method):(myAJAX.constants.defaultValues.method)):(myAJAX.constants.defaultValues.method)).toUpperCase();
     onStartFunction   = (onStartFunction)?((typeof(onStartFunction) == 'function')?(onStartFunction):(null)):(null);
     
     if(newForm) { $remove(newForm); }
     newForm = document.$createElement('form', { 'id': newFormID, 'name': newFormID, 'action': url, 'method': method, 'target': iFrameID, 'style': { 'display': 'none', 'width': '0px', 'height': '0px', 'border': 'none'} }, document.body); 
     if(urlParameters.length > 1)
        { 
         try      { /* assuming that urlParametersKeys is an array */ for(var i = (urlParametersKeys.length - 1); i > -1; i--) { if(document.$createElement('input',{ 'type': 'hidden', 'name': urlParametersKeys[i], 'value': urlParameters.$getKey(urlParametersKeys[i])}, newForm) == null) { throw new Error('Not an array'); } } }
         catch(e) { /* urlParametersKeys isn't an array */ document.$createElement('input',{ 'type': 'hidden', 'name': urlParametersKeys, 'value': urlParameters.$getKey(urlParametersKeys)}, newForm); }
        } 
     if(typeof(onStartFunction) == 'function') { newForm.$captureEvent('submit', onStartFunction); }
     myAJAX._methods._iFrameBugFix4IEDOM(iFrameID); /* this is useful only when the `iframe` was created by standard DOM methods, not with the IE specific method of `createElement` */
     newForm.submit();
     return newFormID;
    }
catch(e) { if(myAJAX.debugMode) { alert('"_addForm" function ERROR: '+e.message); } return false; }  
};
/*/////////////////////////////////////////////////////////////////////*/
var _modifyExistingForm            = function(objectID, form, onStartFunction)
/*///myAJAX (iFrame)///////////////////////////////////////////////////*/
{ /* ----------------------- */
try { 
      var iFrameID = objectID + 'iframe';
      var divID    = objectID + 'div';
      form = $(form);

      if(form && iFrameID)
        {
         form.setAttribute('target',  iFrameID); 
         form.setAttribute('enctype', 'multipart/form-data');
         form.setAttribute('method',  'post');
         try 
                { /* IE Bug Fix  start */
                 if(form.target  != iFrameID)              { form.target  = iFrameID; }
                 if(form.enctype != 'multipart/form-data') { form.enctype = 'multipart/form-data'; }
                 if(form.method  != 'post')                { form.method  = 'post'; }
                 /* IE Bug Fix  end */
                }
         catch(e) { }

         if(typeof(onStartFunction) == 'function') { form.$captureEvent('submit', onStartFunction); }
         form.$captureEvent('reset', function(event) 
                                { 
                                 try { 
                                       var form = ((typeof(event.srcElement) == 'object')?(event.srcElement):((typeof(event.target) == 'object')?(event.target):(null)));
                                       myAJAX.resetClonedFileInputsOnFormReset(form);

                                       var span, hiddenDiv = divID.$();
                                       if((hiddenDiv != null) && hiddenDiv.hasChildNodes())
                                                {
                                                 var formElements = [];
                                                 for(var formElement = hiddenDiv.firstChild;formElement;formElement = formElement.nextSibling)
                                                      { if((formElement.nodeType == myAJAX.constants.XML.NodeType.Element) && formElement.getAttribute('spanID')) { formElements.push(formElement); } }
                                                 for(var i=0, formElementsLength = formElements.length, formElement = formElements[i]; i<formElementsLength; formElement = formElements[++i])
                                                      {
                                                       span = formElement.getAttribute('spanID').$();
                                                       $remove(formElement);
                                                       span.parentNode.replaceChild(formElement, span);
                                                      }
                                                }
                                       return true; 
                                     } 
                                 catch(e) { return false; } 
                                });
        }
      else return false;   
     }
catch(e) { if(myAJAX.debugMode) { alert('"_modifyExistingForm" function ERROR: '+e.message); } return false; }      
};
if(urlParameters == null) { urlParameters = myAJAX._vars._urlParameters[objectID]; }
if(typeof(urlParametersIsReferencingAFormWithFileInput) == 'undefined') { urlParametersIsReferencingAFormWithFileInput = $isFormWithFileInput(urlParameters); }
_addIframe(objectID, onReadyFunction, onErrorFunction, InnerHtmlId);
if(urlParametersIsReferencingAFormWithFileInput) 
        { 
         var returnedValue = _modifyExistingForm(objectID, urlParameters, onStartFunction);
         if(enableStyleEffects) { myAJAX.formElements2span(urlParameters, objectID, true); }
         return returnedValue; 
        }
else    { /* a form must be created */ return _addForm(objectID, url, urlParameters, method, onStartFunction, forceNoCache); }
};
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.draggable = function(object)
/*///myAJAX.draggable//////////////////////////////////////////////////*/ 
{
 try {
      var o = $(object);
      if(o.nodeType == 3) { o = o.parentNode; }
      o.notMoving = true;
      var oXY = o.$getPosition();      
      var delta = {};
      o.style.left = oXY.x + 'px';
      o.style.top =  oXY.y + 'px';      
      o.style.position = 'absolute';
      oXY = null;
  
      var onmousedown = function(event)
        {
         try {
              var o = event.srcElement || event.target;
              try { if(window.event) { window.event.cancelBubble = true; window.event.returnValue = false; } else { event.preventDefault(); } } catch(e) { }
              if(o.notMoving)
                {
                 var oXY =  o.$getPosition();
                 var mXY = $getMousePosition(event);
                 delta.x = mXY.x - oXY.x;
                 delta.y = mXY.y - oXY.y;
                 o.style.cursor = 'move';
                 o.$captureEvent('mousemove',onmousemove, true);
                 o.$captureEvent('mouseup',onmouseup, true);
                 o.notMoving = false;
                } 
             } 
         catch(e) { if(myAJAX.debugMode) { alert('"myAJAX.draggable/onmousedown" function ERROR: ' + e.message); } }    
        };
  
      var onmousemove = function(event)
        {
         try { 
              var o = event.srcElement || event.target;
              try { if(window.event) { window.event.cancelBubble = true; window.event.returnValue = false; } else { event.preventDefault(); } } catch(e) { }
              var mXY = $getMousePosition(event);
              o.style.cursor = 'move';
              o.style.left = (mXY.x - delta.x) + 'px';
              o.style.top =  (mXY.y - delta.y) + 'px';
             }
         catch(e) { if(myAJAX.debugMode) { alert('"myAJAX.draggable/onmousemove" function ERROR: ' + e.message); } }    
        };
  
      var onmouseup = function(event)
        {
         try {
              var o = event.srcElement || event.target;
              o.style.cursor = 'auto';
              o.$releaseEvent('mousemove', onmousemove, true);
              o.$releaseEvent('mouseup',onmouseup, true);  
              o.notMoving = true;
             } 
         catch(e) { if(myAJAX.debugMode) { alert('"myAJAX.draggable/onmouseup" function ERROR: ' + e.message); } } 
        };

      o.$captureEvent('mousedown',onmousedown, true);
      return true;
     }
catch(e) { if(myAJAX.debugMode) { alert('"myAJAX.draggable" function ERROR: ' + e.message); } return false; }      
};
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.XML = function(xmlDoc)
/*///myAJAX.XML////////////////////////////////////////////////////////*/
/* 

Object myAJAX.XML 
/////////////////////////////////////////////////////////////////////////

   PARAMETERS
-------------------------------------------------------------------------
xmlDoc                                                         = XML object returned by responseXML
   
   PROPERTIES
-------------------------------------------------------------------------
 same as PARAMETRS   

   METHODS
-------------------------------------------------------------------------
rootNodeName                                                   = returns root node's name of the XML document
rootNodeObject                                                 = returns root node object of the XML document
nodeObject(nodeName, index=0 | nodeObj)                        = returns node object of the XML document
nodeValue(nodeName,index=0 | nodeObj)                          = returns node's value
nodeAttribute(nodeName, index, attribute | nodeObj, attribute) = returns node's attribute
nodesByTagName(tagName)                                        = returns a list of nodes with specified tag name

/////////////////////////////////////////////////////////////////////////

*/
{
if (!(this instanceof (myAJAX.XML))) return new myAJAX.XML(xmlDoc);
var thisObject = this;
this.xmlDoc    = xmlDoc;
/*/////////////////////////////////////////////////////////////////////*/
this.rootNodeName       = function()
/*///myAJAX.XML////////////////////////////////////////////////////////*/
{
var rootName = thisObject.rootNodeObject().nodeName;
     
if(rootName)  { return rootName; }
else { throw new Error(myAJAX.constants.errorMessages.XMLInvalidDocumentNoRootName); }
};
/*/////////////////////////////////////////////////////////////////////*/  
this.rootNodeObject     = function()
/*///myAJAX.XML////////////////////////////////////////////////////////*/
{
var errMsg = myAJAX.constants.errorMessages;

try { var rootObject = thisObject.xmlDoc.documentElement; }
catch(e) { throw new Error(errMsg.XMLInvalidDocumentNoRootObject); } 
if(typeof(rootObject) == 'object') return rootObject;
else throw new Error(errMsg.XMLInvalidDocumentNoRootObject);
};
/*/////////////////////////////////////////////////////////////////////*/  
this.nodeObject         = function (node /* node's name (string) or node object (object). if node is an object, index is ignored. */, index/* =0 */)
/*///myAJAX.XML////////////////////////////////////////////////////////*/
{
var errMsg = myAJAX.constants.errorMessages;

if(!(node)) throw new Error(errMsg.XMLNodeUndefined);
try {
     var index   = (index == undefined)?(0):(isNaN(index)?(0):(parseInt(index)));
     if($isString(node)) { node = thisObject.xmlDoc.getElementsByTagName(node).item(index); }
     if(typeof(node) == 'object') { return node; }
     else throw new Error(errMsg.XMLNodeObjectNotFound);
    }
catch(e) { throw new Error(errMsg.XMLNodeObjectNotFound); }
};
/*/////////////////////////////////////////////////////////////////////*/  
this.nodeAttribute      = function(node /* node's name (string) or node object (object). if node is an object, index is ignored. */, index/* =0 */, attribute/* = undefined */)
/*///myAJAX.XML////////////////////////////////////////////////////////*/
{
var errMsg = myAJAX.constants.errorMessages;

if(!(node)) throw new Error(errMsg.XMLNodeUndefined);
switch(arguments.length)
        {
                case 3:
                        if(typeof(attribute) == 'object') { attribute = attribute.valueOf(); }
                        var nodeObj = thisObject.nodeObject(node, index);
                        break;
                
                default:
                        attribute = index; 
                        var nodeObj   = thisObject.nodeObject(node);
        }

if((attribute) && (attribute != '*'))
        {
         var attributeValue = null;
         try { attributeValue = nodeObj.attributes[attribute].value; }
         catch(e) 
                {
                 var attributes       = nodeObj.attributes;
                 var attributesLength = attributes.length;
                 for(var i=0; i<attributesLength; i++)
                        if(attributes[i].name==attribute)
                                {
                                 attributeValue = attributes[i].value;
                                 break;
                                }
                 if(attributeValue==null) throw new Error(errMsg.XMLNodeAttributeNotFound);
                }
         return attributeValue;
        }
else { return nodeObj.attributes; }         
};
/*/////////////////////////////////////////////////////////////////////*/  
this.nodeValue          = function(node /* node's name (string) or node object (object). if node is an object, index is ignored. */, index/* = 0 */)
/*///myAJAX.XML////////////////////////////////////////////////////////*/
{
if(!(node)) throw new Error(myAJAX.constants.errorMessages.XMLNodeUndefined);
try {
      return thisObject.nodeObject(node, index).firstChild.nodeValue;
    } 
catch(e)
    {
     throw e;
    }
};
/*/////////////////////////////////////////////////////////////////////*/  
this.nodesByTagName     = function(tagName)
/*///myAJAX.XML////////////////////////////////////////////////////////*/
{
try      { return thisObject.xmlDoc.getElementsByTagName(tagName); }
catch(e) { throw new Error(myAJAX.constants.errorMessages.XMLTagNameNotFound); }     
};
/*/////////////////////////////////////////////////////////////////////*/  
};
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/  
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.XMLHttp = function()
/*///myAJAX.XMLHttp////////////////////////////////////////////////////*/
/* 

Object myAJAX.XMLHttp 
/////////////////////////////////////////////////////////////////////////

   PARAMETERS
-------------------------------------------------------------------------
none
   
   PROPERTIES
-------------------------------------------------------------------------
requestObject               = returns XMLHttp / Microsoft.XmlHttp object

   METHODS
-------------------------------------------------------------------------
none

/////////////////////////////////////////////////////////////////////////

*/
{
if (!(this instanceof (myAJAX.XMLHttp))) { return new myAJAX.XMLHttp(); }
var XMLHttpObject = false;

if(typeof(XMLHttpRequest) != 'undefined')  { XMLHttpObject = new XMLHttpRequest(); }
else {
      if(window.ActiveXObject) 
        {
         var versions = ["Microsoft.XmlHttp", "MSXML2.XmlHttp","MSXML2.XmlHttp.3.0", "MSXML2.XmlHttp.4.0","MSXML2.XmlHttp.5.0","MSXML2.XmlHttp.6.0","MSXML2.XmlHttp.7.0"];
         for (var i = (versions.length - 1); i > -1; i--) 
                {
                 try {
                       XMLHttpObject = new ActiveXObject(versions[i]);
                       break;
                      } 
                 catch(e) {}
                }
        }
      else throw new Error(myAJAX.constants.errorMessages.NoAJAX);
     }
/*/////////////////////////////////////////////////////////////////////*/
try      { this.__defineGetter__('requestObject', function() { return XMLHttpObject; }); } 
catch(e) { this.requestObject = XMLHttpObject; }
/*/////////////////////////////////////////////////////////////////////*/
};
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/  
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.constants      = 
        {
         HTTP:  
                { 
                        readyState: 
                                { 
                                        Uninitialized: 0/* The object has been created but the open() method hasn't been called. */, 
                                        Loading: 1/* The open() method has been called but the request hasn't been sent. */, 
                                        Loaded: 2/* The request has been sent. */, 
                                        Interactive: 3/* A partial response has been received. */, 
                                        Complete: 4/* All data has been received and the connection has been closed */ 
                                }, 
                        serverStatus: 
                                { 
                                        FileOK: 0, 
                                        OK: 200, 
                                        Created: 201, 
                                        NoContent: 204, 
                                        ResetContent: 205, 
                                        PartialContent: 206, 
                                        BadRequest: 400, 
                                        Unauthorized: 401, 
                                        Forbidden: 403, 
                                        NotFound: 404, 
                                        MethodNotAllowed: 405, 
                                        NotAcceptable: 406, 
                                        ProxyAuthenticationRequired: 407, 
                                        RequestTimeout: 408, 
                                        LengthRequired: 411, 
                                        RequestedEntityTooLarge: 413, 
                                        RequestedURLTooLong: 414, 
                                        UnsupportedMediaType: 415, 
                                        InternalServerError: 500, 
                                        NotImplemented: 501, 
                                        BadGateway: 502, 
                                        ServiceUnavailable: 503, 
                                        GatewayTimeout: 504, 
                                        HTTPVersionNotSupported:505 
                                } 
                },
        
         XML : 
                { 
                        NodeType: 
                                { 
                                        Element: 1, 
                                        Attribute: 2, 
                                        Text: 3, 
                                        CDataSection: 4, 
                                        EntityReference: 5, 
                                        Entity: 6, 
                                        ProcessingInstruction: 7, 
                                        Comment: 8, 
                                        Document: 9, 
                                        DocumentType: 10, 
                                        DocumentFragment: 11, 
                                        Notation: 12 
                                }, 
                        regExpTestXML: new RegExp ('^text/xml$|^application/xml$|^application/([a-z0-9]+[+]{1})xml$','gi') 
                },
                
         ajaxMethod: 
                { 
                                        auto: '', 
                                        XMLHttp: 'XMLHttp', 
                                        iFrame: 'iFrame' 
                },
         
         defaultValues:
                { 
                                        method: 'GET', 
                                        refreshInterval: 0, 
                                        forceNoCache: true, 
                                        enableMultiplyingFileInputs: false 
                },
         
         errorMessages:
                { 
                                        NoAJAX: "XMLHttp: XMLHttp (AJAX) not supported!", 
                                        iframeElementNotSupported: "myAJAX.sendRequest: `iframe` element not supported in browser!",
                                        unexpectedError: "Unexpected error!", 
                                        InstanceNameUndefined: "myAJAX.namedInstance: `instanceName` parameter is not set!", 
                                        UrlUndefined: "myAJAX.sendRequest: `url` parameter is not set!", 
                                        XMLNodeUndefined: "myAJAX.XML: `node` parameter is not set!", 
                                        XMLInvalidDocumentNoRootName: "myAJAX.XML.rootNodeName: Invalid XML document (root node name not found)!", 
                                        XMLInvalidDocumentNoRootObject: "myAJAX.XML.rootNodeObject: Invalid XML document (root node object not found)!", 
                                        XMLNodeObjectNotFound: "myAJAX.XML.nodeObject: Node object not found!", 
                                        XMLNodeAttributeNotFound: "myAJAX.XML.nodeAttribute: Node attribute not found!", 
                                        XMLTagNameNotFound: "myAJAX.XML.nodesByTagName: Tag name not found!" 
                }
         
        };
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/
myAJAX.constants.ajaxMethod.regExpTest    =  new RegExp ('^' + myAJAX.constants.ajaxMethod.XMLHttp + '$|^' + myAJAX.constants.ajaxMethod.iFrame + '$','i');
myAJAX.constants.ajaxMethod.auto          = myAJAX.constants.ajaxMethod.auto.toUpperCase();
myAJAX.constants.ajaxMethod.XMLHttp       = myAJAX.constants.ajaxMethod.XMLHttp.toUpperCase();
myAJAX.constants.ajaxMethod.iFrame        = myAJAX.constants.ajaxMethod.iFrame.toUpperCase();
myAJAX.constants.defaultValues.ajaxMethod = myAJAX.constants.ajaxMethod.XMLHttp;
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/  
/*/////////////////////////////////////////////////////////////////////*/
/* variables defined for compatibility with previous versions */
/*/////////////////////////////////////////////////////////////////////*/
var myXML   = myAJAX.XML;
var XMLHttp = myAJAX.XMLHttp;
/*/////////////////////////////////////////////////////////////////////*/
