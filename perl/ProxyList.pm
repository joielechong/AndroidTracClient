{    package ProxyList;
    require Exporter;
    @ISA = qw(Exporter);
    
    use Data::Dumper;
    
    BEGIN {
			$ProxyList::VERSION = "0.1";

our @proxies = (
    '58.240.237.32:80',
    '60.12.227.209:3128',
    '61.172.246.180:80',
#    '61.172.249.96:80',
    '63.220.6.34:80',
    '64.125.136.28:80',
#    '66.90.234.124:8080',
    '67.228.115.150:80',
    '69.162.118.246:3128',
    '83.236.237.163:3128',
    '84.255.246.20:80',
    '87.66.29.96:80',
    '89.248.194.212:3128',
    '94.76.201.172:3128',
    '119.70.40.101:8080',
    '148.233.239.23:80',
    '150.188.31.2:3128',
#   '189.70.29.113:80',
#   '189.109.46.210:3128',
    '189.123.117.246:8080',
    '190.90.27.30:8080',
    '190.199.230.74:3128',
    '200.14.96.57:80',
#    '200.30.101.2:8080',
    '200.54.145.98:8080',
    '200.61.25.1:8080',
#    '200.148.230.217:3128',
    '200.174.85.195:3128',
    '202.54.61.99:8080',
    '202.142.145.138:8080',
    '202.171.26.146:8080',
    '203.160.1.94:80',
    '207.250.81.2:3128',
#    '211.115.185.41:80',
    '211.115.185.42:80',
#    '217.58.153.172:8080',
    '219.134.186.106:3128',
    '189.53.181.7:80',
    '60.12.227.209:3128',
    '221.192.132.194:80',
    '203.160.1.121:80',
    '212.102.0.104:80',
    '82.115.86.162:3128',
    '189.39.115.185:3128',
    '200.30.101.20:8080',
    '203.160.1.75:80',
    '88.191.80.237:3128',
    '89.212.253.19:8080',
    '64.251.57.198:8080',
    '212.44.145.21:80',
    '200.139.78.114:3128',
    '217.23.180.98:3128',
    '66.63.165.9:3128',
    '218.24.180.12:80',
    '77.73.162.21:3128',
    '202.111.189.159:3128',
    '77.244.218.34:3128',
    '190.139.101.154:8080',
    '77.71.0.149:8080',
#    '189.50.116.113:8080',
    '218.56.64.210:8080',
    '94.84.179.4:3128',
    '190.97.144.194:6588',
    '118.220.175.207:80',
    '200.35.163.212:3128',
    '80.148.22.116:8080',
    '61.172.249.94:80',
    '86.61.211.47:8080',
    '216.101.231.130:8000',
    '222.247.62.195:8080',
    '193.171.32.6:80',
#    '202.98.23.114:80',
    '202.98.23.116:80',
    '124.40.121.7:80',
    '201.20.89.10:3128',
    '203.160.1.85:80',
    '59.120.244.23:3128',
    '196.219.18.34:80',
#    '212.143.227.248:3128',
    '125.41.181.59:8080',
    '98.243.10.242:80',
    '195.54.22.74:8080',
    '221.6.62.90:8080',
    '189.45.48.7:8080',
    '200.174.85.195:3128',
    '200.204.154.29:6588',
    '209.17.186.25:8080',
#    '217.23.68.2:8080',
    '211.115.185.44:80',
    '92.63.49.201:8080',
    '190.27.218.105:8080',
    '77.242.233.102:8080',
    '199.193.13.202:80',
    '211.99.188.218:80',
    '203.160.1.112:80',
    '89.207.211.66:80',
    '203.162.183.222:80',
    '202.152.27.130:8080',
    '124.81.224.174:8080',
    '199.71.215.50:3128',
    '122.226.12.28:3128',
    '123.233.121.164:80',
    '59.36.98.154:80',
    '199.71.213.7:3128',
    '77.242.33.5:80',
    '98.192.125.23:80',
    '190.254.85.211:3128',
    '94.23.47.56:3128',
    '83.2.212.9:8080',
    '219.134.186.106:3128',
    '217.23.180.98:3128',
    '120.143.250.8:80',
    '190.199.230.74:3128',
    '202.95.128.126:3128',
    '69.162.118.246:3128',
    '217.70.61.54:3128',
    '80.68.95.142:3128',
    '200.204.154.29:6588',
    '192.116.226.69:8080',
    '66.63.165.7:3128',
    '61.172.246.180:80',
    '148.233.239.23:80',
    '190.139.101.154:8080',
    '211.115.185.42:80',
    '212.1.95.50:8080',
    '62.134.53.182:3128',
    '203.160.001.103:80',
    '200.45.199.189:8080',
#    '81.187.204.161:6588',
    '77.244.218.34:3128',
    '89.248.194.212:3128',
    '63.220.6.34:80',
    '94.76.201.172:3128',
    '203.162.183.222:80',
    '203.160.1.75:80',
    '77.242.233.44:8080',
    '59.36.98.154:80',
    '203.99.131.186:8080',
    '189.127.143.70:3128',
    '189.50.116.113:8080',
    '195.54.22.74:8080',
    '189.56.61.33:3128',
    '218.242.239.61:8080',
    '61.172.249.94:80',
    '118.220.175.207:80',
    '200.179.88.180:3128',
    '202.152.27.130:8080',
    '86.61.211.47:8080',
    '202.248.42.25:80',
    '80.148.17.149:80',
    '212.44.145.21:80',
    '190.27.218.105:8080',
    '202.111.189.159:3128',
    '98.192.125.23:80',
    '203.160.1.121:80',
    '189.50.119.1:8080',
    '150.188.31.2:3128',
    '209.17.186.25:8080',
    '98.243.10.242:80',
    '202.71.98.201:3128',
    '59.39.145.178:3128',
    '77.73.162.21:3128',
    '200.199.25.178:3128',
    '82.115.86.162:3128',
    '69.197.153.46:3128',
    '200.148.230.217:3128',
    '200.61.25.1:8080',
    '201.222.99.12:3128',
    '84.255.246.20:80',
    '66.63.165.9:3128',
    '81.18.116.70:8080',
    '218.101.6.204:80',
    '88.191.80.237:3128',
    '207.250.81.2:3128',
    '211.115.185.44:80',
    '58.65.240.10:3128',
    '203.160.1.112:80',
    '202.171.26.146:8080',
    '212.102.0.104:80',
    '110.8.253.100:80',
    '59.120.244.23:3128',
    '79.170.43.72:3128',
    '122.226.12.28:3128',
    '203.160.1.66:80',
    '200.174.85.193:3128',
    '119.235.25.242:8080',
    '121.97.128.19:3128',
    '190.102.206.48:8080',
    '141.85.118.1:80',
    '84.92.192.146:3128',
    '119.70.40.102:8080',
#    '201.73.45.70:3128',
    '58.56.147.42:3128',
#    '213.180.131.135:80',
    '62.168.41.61:3128',
    '221.6.62.90:8080',
    '213.180.131.135:80',
    '203.160.1.85:80',
    '60.253.114.30:3128',
    '211.215.17.56:80',
    '67.228.115.150:80',
    '64.125.136.28:80',
    '62.168.41.61:3128',
    '58.56.147.42:3128',
#    '87.66.29.96:80',
    '60.253.114.30:3128',
    '203.160.1.85:80',
    '211.215.17.56:80',
    '221.6.62.90:8080',
    '64.125.136.28:80',
    '67.228.115.150:80'
    );
	}
	
	sub get_proxy() {
		return $proxies[int(rand(1+$#proxies))];
	}
}	
1;
