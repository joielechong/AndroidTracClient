/**
*
* myAJAX:  My AJAX implemetation ( http://myajax.sourceforge.net/ )
* Copyright (c) 2006 - 2007, Raul IONESCU <raul.ionescu@yahoo.com>, Bucharest, ROMANIA
*
* @package      IBAN check
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
* @category 	IBAN validity checker.
* @access 	public
*
* REDISTRIBUTIONS OF FILES MUST RETAIN THE ABOVE COPYRIGHT NOTICE.
*/



/*/////////////////////////////////////////////////////////////////////*/
function checkIBAN(iban) 
/*/////////////////////////////////////////////////////////////////////*/
{
try {
     var tmp       = (typeof(iban) == 'undefined')?(this.toUpperCase()):(iban.toUpperCase());
     var tmpLength = tmp.length;
     if((tmpLength == undefined) || (tmpLength < 8)) { return false; }
     var sCheck    = new String(tmp.substring(4,8) + tmp.substring(8, tmpLength) + tmp.substring(0,2) + tmp.substring(2,4));
     var newS      = new String('');
     var n;

     for (var i = 0; i<tmpLength; ++i ) 
	{
	 var a = sCheck.charCodeAt(i);
	 if ((a >= 65) && (a <= 90)) { n = a - 55; }
	 else { n = sCheck.charAt(i); }
	 newS += n; 
	}
     var newSLength = newS.length;
     var newM       = parseInt(newS.substring(0,2),10);
     var r          = newM % 97;
     for (i = 2; i < newSLength; ++i) 
	{
	 newM = 10*r + parseInt(newS.substring(i, i+1),10);
	 r = newM % 97;
	}
     return (r==1);
    }
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
String.prototype.checkIBAN = checkIBAN;
/*/////////////////////////////////////////////////////////////////////*/