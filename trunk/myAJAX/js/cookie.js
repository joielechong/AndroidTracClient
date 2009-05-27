/**
*
* myAJAX:  My AJAX implemetation ( http://myajax.sourceforge.net/ )
* Copyright (c) 2006 - 2007, Raul IONESCU <raul.ionescu@yahoo.com>, Bucharest, ROMANIA
*
* @package      Cookie
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
* @category 	Cookie
* @access 	public
*
* REDISTRIBUTIONS OF FILES MUST RETAIN THE ABOVE COPYRIGHT NOTICE.
*/



function Cookie(name/*, value = null, days = null*/)
/*///Cookie////////////////////////////////////////////////////////////*/ 
/* 

Object Cookie 
/////////////////////////////////////////////////////////////////////////

   PARAMETERS
-------------------------------------------------------------------------
name  = cookie's name
   
   PROPERTIES
-------------------------------------------------------------------------
none

   METHODS
-------------------------------------------------------------------------
set(value, days) = sets cookie's value
get()            = get cookie's value
remove()         = remove cookie

/////////////////////////////////////////////////////////////////////////

*/
{
if (!(this instanceof (Cookie))) { return new Cookie(name); }

/*/////////////////////////////////////////////////////////////////////*/
this.set     = function(value, days) { setCookie(this.name, value, days); };
this.get     = function() { return getCookie(this.name); };
this.remove  = function() { removeCookie(this.name); };
try {
      this.__defineGetter__('name', function() { return _name; });
      this.__defineSetter__('name', function(n) { _name = n; });
      var _name = name;
     } 
catch(e) 
     { this.name = name; }
};
/*/////////////////////////////////////////////////////////////////////*/
Cookie.constants                           = new Object();
Cookie.constants.errorMessages             = new Object();
Cookie.constants.errorMessages.nameNotSet  = new Error('Cookie name not set!');
Cookie.constants.errorMessages.valueNotSet = new Error('Cookie value not set!');
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/
/*/////////////////////////////////////////////////////////////////////*/
function setCookie(name,value,days) 
/*///Cookie////////////////////////////////////////////////////////////*/
{
if (!(name))  throw Cookie.constants.errorMessages.nameNotSet;
if(value == undefined) throw Cookie.constants.errorMessages.valueNotSet;

if (days != undefined)	
        {
	 var date = new Date();
	 date.setTime(date.getTime() + (days*86400000));
	 var expires = '; expires=' + date.toGMTString();
	}
else var expires = '';
document.cookie = name + '=' + value + expires + '; path=/;';
};
/*/////////////////////////////////////////////////////////////////////*/  
function getCookie(name) 
/*///Cookie////////////////////////////////////////////////////////////*/
{
if (!(name))  throw Cookie.constants.errorMessages.nameNotSet;
var cookies       = document.cookie.split(';');
var cookiesLength = cookies.length;

for(var i = 0;i < cookiesLength;i++) 
        {
	 var currentCookieNameAndValue = cookies[i].split('=');
	 if((currentCookieNameAndValue[0] == name) || (currentCookieNameAndValue[0] == ' ' + name)) { return (currentCookieNameAndValue.length > 1)?(currentCookieNameAndValue[1]):(null); }
	}
return null;
};
/*/////////////////////////////////////////////////////////////////////*/  
function removeCookie(name) 
/*///Cookie////////////////////////////////////////////////////////////*/
{ 
if (!(name))  throw Cookie.constants.errorMessages.nameNotSet;
setCookie(name,'',-1); 
};
/*/////////////////////////////////////////////////////////////////////*/ 
/*/////////////////////////////////////////////////////////////////////*/  
/*/////////////////////////////////////////////////////////////////////*/
