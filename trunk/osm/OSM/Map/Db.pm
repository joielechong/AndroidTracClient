{
    package OSM::Map::Db;
   
    use strict;
    use vars qw(@ISA $VERSION);
    
    use DBI;
    use Geo::Distance;

    require Exporter;
    @ISA = qw(Exporter DBI);
    
    my $dbh;
    
    sub new {
        my $this = shift;
	my $conffile = shift;
	my $class = ref($this) || $this;
	my $self = {};
	bless $self, $class;
	$self->initialize($conffile);
	return $self;
    }
    
    my $geo;
    my $getcoor;
    my $getcounts;
    my $insertnode;
    my $delnode;
    my $checknode;
    my $adminnode;
    my $latlonarr;
    my $inboundcoor;
    my $loadbucket;
    my $insertway;
    my $insertnd;
    my $getnb;
    my $checkway;
    my $delway;
    my $inserttag;
    my $inboundnd;
    my $getdist;
    my $storedist;
    
    sub initialize {
        my $self = shift;
        my $naam = shift;
        
        $dbh = $self->{dbh} = DBI->connect("dbi:SQLite:dbname=$naam","","");
        $dbh->{AutoCommit} = 1;
        $dbh->do("PRAGMA foreign_keys=ON");
        $checknode = $dbh->prepare("SELECT version from node where id =?");
        $self->{checkrel}  = $dbh->prepare("SELECT version from relation where id =?");
        $checkway  = $dbh->prepare("SELECT version from way where id =?");

        $delnode = $dbh->prepare("DELETE from node where id =?");
        $self->{delrel}  = $dbh->prepare("DELETE from relation where id =?");
        $delway  = $dbh->prepare("DELETE from way where id =?");

        $insertnode  = $dbh->prepare("INSERT INTO node (id,lat,lon,version,temporary) VALUES (?,?,?,?,?)");
        $inserttag   = $dbh->prepare("INSERT OR REPLACE INTO tag (id,k,v) VALUES (?,?,?)");
        $insertway   = $dbh->prepare("INSERT INTO way (id,version,temporary) VALUES (?,?,?)");
        $insertnd    = $dbh->prepare("INSERT INTO nd (id,seq,ref) VALUES (?,?,?)");
        $self->{insertrel}   = $dbh->prepare("INSERT INTO relation (id,version) VALUES (?,?)");
        $self->{insertmemb}  = $dbh->prepare("INSERT INTO member (id,seq,type,ref,role) VALUES (?,?,?,?,?)");
        $self->{insertbound} = $dbh->prepare("INSERT INTO bound (minlat,maxlat,minlon,maxlon) VALUES (?,?,?,?)");
        $self->{insertnb}    = $dbh->prepare("INSERT INTO neighbor (id1,id2,way) VALUES (?,?,?)");
        
	$inboundnd   = $dbh->prepare("SELECT count(maxlat) FROM bound,node WHERE id=? and lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon");
	$inboundcoor = $dbh->prepare("SELECT count(maxlat) FROM bound,(SELECT ? as lat,? as lon) as input WHERE lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon");
	$adminnode   = $dbh->prepare("SELECT admin.id,name,level FROM admin,node WHERE node.id=? and lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon ORDER BY level DESC,name");
	$getcounts   = $dbh->prepare("SELECT * FROM counts");
	$getcoor     = $dbh->prepare("SELECT lat,lon FROM node WHERE id=?");
	$getnb       = $dbh->prepare("SELECT n.id2 FROM (SELECT neighbor.* FROM neighbor UNION SELECT id2,id1,way,distance FROM neighbor) AS n,(SELECT ? AS input) AS x WHERE input=n.id1");
	$latlonarr   = $dbh->prepare("SELECT lat,lon FROM member,nd,node WHERE member.id=? AND member.type='way' AND member.ref=nd.id AND nd.ref=node.id ORDER BY member.seq,nd.seq");
        $loadbucket  = $dbh->prepare("SELECT b1.node,lat,lon FROM bucket AS b2,bucket AS b1,node WHERE b2.node=? AND b2.x=b1.x AND b2.y=b1.y AND b1.node != b2.node AND b1.node=id");
	$getdist     = $dbh->prepare("SELECT distance FROM neighbor AS nb, (SELECT ? AS id1,? AS id2) AS inp WHERE (nb.id1=inp.id1 AND nb.id2=inp.id2) OR (nb.id1=inp.id2 AND nb.id2=inp.id1)");
	$storedist   = $dbh->prepare("UPDATE neighbor set distance=? WHERE id1=? AND id2=?");
 
#        $dbh->do("DELETE FROM relation WHERE NOT processed");
#        $dbh->do("DELETE FROM way WHERE NOT processed");
#        $dbh->do("DELETE FROM node WHERE NOT processed");
        $geo=new Geo::Distance;
    }
    
    sub distance {
	my ($self,$n1,$n2) = @_;
	return 0 if $n1 < 0 or $n2 < 0;
	my $row = $dbh->selectcol_arrayref($getdist,{},$n1,$n2);
	return $row->[0] if defined $row->[0];
	my ($lat1,$lon1) = $self->getCoor($n1);
	my ($lat2,$lon2) = $self->getCoor($n2);
        my $d = $geo->distance('meter',$lon1,$lat1=>$lon2,$lat2);
	$storedist->execute($d,$n1,$n2);
	$storedist->execute($d,$n2,$n1);
	return $d;
   }
    
    sub insertTag {
        my ($self,$id,$k,$v) = @_;
	$inserttag->execute($id,$k,$v);
    }
    
    sub inboundNode {
        my ($self,$node) = @_;
	$inboundnd->execute($node);
        if (my @row = $inboundnd->fetchrow_array()) {
             return $row[0];
        }
	return 0;
    }
    
    sub inboundCoor {
        my $self = shift;
        $inboundcoor->execute(@_);
        if (my @row = $inboundcoor->fetchrow_array()) {
             return $row[0];
        }
	return 0;
    }
    
    sub do {
        my $self = shift;
        return $dbh->do(shift);
    }
    
    sub getLatLonarr {
        my ($self,$id) = @_;
        
        $latlonarr->execute($id);
        return $latlonarr->fetchall_arrayref();
    }

    sub getCoor {
        my ($self,$node) = @_;
        
    	$getcoor->execute($node);
	return $getcoor->fetchrow_array();
    }
    
    sub getCounts {
	my $self = shift;
	
	$getcounts->execute();
	return $getcounts->fetchrow_array();
    }
    
    sub insertNode {
        my $self = shift;
        $insertnode->execute(@_);
    }
    
    sub insertWay {
        my $self = shift;
        $insertway->execute(@_);
    }
    
    sub insertNd {
        my $self = shift;
        $insertnd->execute(@_);
    }
    
    sub checkNode {
        my ($self,$id) = @_;
        $checkway->execute($id);
        return $checkway->fetchrow_array();
    }
    
    sub checkWay {
        my ($self,$id) = @_;
        $checknode->execute($id);
        return $checknode->fetchrow_array();
    }
    
    sub delNode {
        my ($self,$id) = @_;
        $delnode->execute($id);
    }
    
    sub delWay {
        my ($self,$id) = @_;
        $delway->execute($id);
    }
    
    sub adminNode {
        my ($self,$node) = @_;
       $adminnode->execute($node);
       return $adminnode->fetchall_arrayref();
    }
    
    sub minID {
        my $self = shift;
        
        my $row = $dbh->selectcol_arrayref("SELECT min(id) from way UNION SELECT min(id) from node");
        return $row->[0];
    }
    
    sub getNb {
        my ($self,$node) = @_;
        
        return $dbh->selectcol_arrayref($getnb,{},$node);
    }
    
    sub loadBucket{
        my ($self,$node) = @_;
        
        $loadbucket->execute($node);
        return $loadbucket->fetchall_arrayref();
    }    
}
1;