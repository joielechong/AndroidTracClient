{
    package OSM::Map::Db;
   
    use strict;
    use vars qw(@ISA $VERSION);
    
    use DBI;
    use Geo::Distance;
    use Data::Dumper;
    
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
    my $getway;
    my $checkway;
    my $delway;
    my $inserttag;
    my $inboundnd;
    my $getdist;
    my $storedist;
    my $insertrel;
    my $insertmemb;
    my $insertnb;
    my $insertbound;
    my $checkrel;
    my $delrel;
    my $loadway;
    my $loadrel;
    my $loadnode;
    my $loadnd;
    my $loadtags;
    my $loadmemb;
    my $getways;
    my $boundcnt;
    
    sub initialize {
        my $self = shift;
        my $naam = shift;
        
        $dbh = $self->{dbh} = DBI->connect("dbi:SQLite:dbname=$naam","","");
        $dbh->{AutoCommit} = 1;
        $dbh->do("PRAGMA foreign_keys=ON");
        $checknode = $dbh->prepare("SELECT version from node where id =?");
        $checkrel  = $dbh->prepare("SELECT version from relation where id =?");
        $checkway  = $dbh->prepare("SELECT version from way where id =?");

        $delnode = $dbh->prepare("DELETE from node where id =?");
        $delrel  = $dbh->prepare("DELETE from relation where id =?");
        $delway  = $dbh->prepare("DELETE from way where id =?");

        $insertnode  = $dbh->prepare("INSERT INTO node (id,lat,lon,version,x,y) VALUES (?,?,?,?,?,?)");
        $inserttag   = $dbh->prepare("INSERT OR REPLACE INTO tag (id,k,v) VALUES (?,?,?)");
        $insertway   = $dbh->prepare("INSERT INTO way (id,version) VALUES (?,?)");
        $insertnd    = $dbh->prepare("INSERT INTO nd (id,seq,ref) VALUES (?,?,?)");
        $insertrel   = $dbh->prepare("INSERT INTO relation (id,version) VALUES (?,?)");
        $insertmemb  = $dbh->prepare("INSERT OR REPLACE INTO member (id,seq,type,ref,role) VALUES (?,?,?,?,?)");
        $insertbound = $dbh->prepare("INSERT OR IGNORE INTO bound (minlat,maxlat,minlon,maxlon) VALUES (?,?,?,?)");
        $insertnb    = $dbh->prepare("INSERT INTO neighbor (id1,id2,way) VALUES (?,?,?)");
        
        $loadway     = $dbh->prepare("SELECT version FROM way where id=?");
        $loadtags    = $dbh->prepare("SELECT k,v FROM tag WHERE id=?");
        $loadnd      = $dbh->prepare("SELECT seq,ref FROM nd WHERE id=?");
        $loadnode    = $dbh->prepare("SELECT lat,lon,version FROM node WHERE id=?");
        $loadrel     = $dbh->prepare("SELECT version FROM relation WHERE id=?");
        $loadmemb    = $dbh->prepare("SELECT seq,type,ref,role FROM member WHERE id=?");
	$inboundnd   = $dbh->prepare("SELECT count(maxlat) FROM bound,node WHERE id=? and lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon");
	$inboundcoor = $dbh->prepare("SELECT count(maxlat) FROM bound,(SELECT ? as lat,? as lon) as input WHERE lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon");
	$adminnode   = $dbh->prepare("SELECT admin.id,name,level FROM admin,node WHERE node.id=? and lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon ORDER BY level DESC,name");
	$getcounts   = $dbh->prepare("SELECT * FROM counts");
	$getcoor     = $dbh->prepare("SELECT lat,lon FROM node WHERE id=?");
	$getnb       = $dbh->prepare("SELECT id1,id2 FROM neighbor, (SELECT ? AS input) AS x WHERE input=id1 OR input=id2");
	$latlonarr   = $dbh->prepare("SELECT lat,lon FROM member,nd,node WHERE member.id=? AND member.type='way' AND member.ref=nd.id AND nd.ref=node.id ORDER BY member.seq,nd.seq");
        $loadbucket  = $dbh->prepare("SELECT b1.node,lat,lon FROM bucket AS b2,bucket AS b1,node WHERE b2.node=? AND b2.x=b1.x AND b2.y=b1.y AND b1.node != b2.node AND b1.node=id");
	$getdist     = $dbh->prepare("SELECT distance FROM neighbor AS nb, (SELECT ? AS id1,? AS id2) AS inp WHERE (nb.id1=inp.id1 AND nb.id2=inp.id2) OR (nb.id1=inp.id2 AND nb.id2=inp.id1)");
	$getway      = $dbh->prepare("SELECT way FROM neighbor AS nb, (SELECT ? AS id1,? AS id2) AS inp WHERE (nb.id1=inp.id1 AND nb.id2=inp.id2) OR (nb.id1=inp.id2 AND nb.id2=inp.id1)");
	$getways     = $dbh->prepare("SELECT way FROM neighbor AS nb, (SELECT ? AS id) AS inp WHERE id1=id  OR id2=id");
	$storedist   = $dbh->prepare("UPDATE neighbor set distance=? WHERE id1=? AND id2=?");
	$boundcnt    = $dbh->prepare("SELECT count(*) FROM bound");
 
#        $dbh->do("DELETE FROM relation WHERE NOT processed");
#        $dbh->do("DELETE FROM way WHERE NOT processed");
#        $dbh->do("DELETE FROM node WHERE NOT processed");
        $geo=new Geo::Distance;
    }
    
    sub getWay {
 	my ($self,$n1,$n2) = @_;
	my $row = $dbh->selectcol_arrayref($getway,{},$n1,$n2);
	return $row->[0] if defined $row->[0];
    }
   
    sub getWays {
 	my ($self,$n1) = @_;
	return $dbh->selectcol_arrayref($getways,{},$n1);
    }
   
    sub loadWay {
        my ($self,$w) = @_;
        
        my $row = $dbh->selectcol_arrayref($loadway,{},$w);
        return undef unless defined $row->[0];
        my $elem;
        $elem->{xmlname}='way';
        $elem->{id} = $w;
        $elem->{version} = $row->[0];
        $loadtags->execute($w);
        while (my @row = $loadtags->fetchrow_array) {
            $elem->{tag}->{$row[0]}=$row[1];
        }
        $loadnd->execute($w);
        while (my @row = $loadnd->fetchrow_array) {
            $elem->{nd}->[$row[0]]={ref=>$row[1]};
        }
        return $elem;
    }
    
    sub loadNode {
        my ($self,$n) = @_;
        
        my $row = $dbh->selectcol_arrayref($loadnode,{},$n);
        return undef unless defined $row->[0];
        my $elem;
        $elem->{xmlname}='node';
        $elem->{id} = $n;
        $elem->{version} = $row->[0];
        $loadtags->execute($n);
        while (my @row = $loadtags->fetchrow_array) {
            $elem->{tag}->{$row[0]}=$row[1];
        }
        return $elem;
    }
    
    sub getDistance {
	my ($self,$n1,$n2) = @_;
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
	if ($k eq 'oneway' or $k eq 'bridge' or $k eq 'tunnel') {
	    return if ($v eq '0' or $v eq 'no' or $v eq 'NO');
	    $v = 'yes' if ($v eq '1' or $v eq 'true' or $v eq 'TRUE' or $v eq 'YES');
	    $v = 'rev' if ($v eq '-1' and $k eq 'oneway');
	}
	$inserttag->execute($id,$k,$v) unless ($k =~ /^source/ or $k eq 'converted_by' or $k eq 'created_by' or $k =~ /^3dshapes/ or $k eq 'time' or $k eq 'timestamp' or $k eq 'user' or $k =~ /^AND/ or $k =~ /^note/ or $k =~ /^openGeoDB/ or $k =~ /^opengeodb/ or $k eq 'fixme' or $k eq 'FIXME' or $k eq 'todo' or $k eq 'TODO' );
    }
    
    sub boundCount {
	my $self = shift;

	$boundcnt->execute();
	my @count = $boundcnt->fetchrow_array();
	return $count[0];
    }
    
    sub inboundNode {
        my ($self,$node) = @_;

	return 1 if $self->boundCount() == 0;
	$inboundnd->execute($node);
        if (my @row = $inboundnd->fetchrow_array()) {
             return $row[0];
        }
	return 0;
    }
    
    sub inboundCoor {
        my $self = shift;
	my $lat = shift;
	my $lon = shift;

	return 1 if $self->boundCount() == 0;
        $inboundcoor->execute($lat,$lon);
        if (my @row = $inboundcoor->fetchrow_array()) {
             return $row[0];
        }
	return 0;
    }
    
    sub do {
        my $self = shift;
        print "DB: ",join(",",@_),"\n";
        return $dbh->do(@_);
    }
    
    sub begin_work {
        $dbh->begin_work;
    }
    
    sub commit {
        $dbh->commit;
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
	my @counts = $getcounts->fetchrow_array();
	foreach (@counts) {
	    $_ = 0 unless defined;
	}
	return @counts;
    }

    sub insertNode {
        my ($self,$id,$lat,$lon,$version) = @_;
        $insertnode->execute($id,$lat,$lon,$version,round(($lat+90)*20),round(($lon+180)*20));
    }
    
    sub insertWay {
        my $self = shift;
        $insertway->execute(@_);
    }
    
    sub insertRelation {
        my $self = shift;
        $insertrel->execute(@_);
    }
    
    sub insertBound {
        my $self = shift;
        $insertbound->execute(@_);
    }
    
    sub insertNd {
        my $self = shift;
        $insertnd->execute(@_);
    }
    
    sub insertMemb {
        my $self = shift;
        $insertmemb->execute(@_);
    }
    
    sub insertNb {
        my $self = shift;
        $insertnb->execute(@_);
    }
    
    sub checkNode {
        my ($self,$id) = @_;
        $checknode->execute($id);
        return $checknode->fetchrow_array();
    }
    
    sub checkWay {
        my ($self,$id) = @_;
        $checkway->execute($id);
        return $checkway->fetchrow_array();
    }
    
    sub checkRelation {
        my ($self,$id) = @_;
        $checkrel->execute($id);
        return $checkrel->fetchrow_array();
    }
    
    sub delNode {
        my ($self,$id) = @_;
        $delnode->execute($id);
    }
    
    sub delWay {
        my ($self,$id) = @_;
        $delway->execute($id);
    }
    
    sub delRelation {
        my ($self,$id) = @_;
        $delrel->execute($id);
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
        my @result=();
        
        $getnb->execute($node);
        while (my @row = $getnb->fetchrow_array) {
            push @result,$row[1] if $row[0] == $node;
            push @result,$row[0] if $row[1] == $node;
        }
        return \@result;
    }
    
    sub loadBucket{
        my ($self,$node) = @_;
        
        $loadbucket->execute($node);
        return $loadbucket->fetchall_arrayref();
    }    
    
    sub imcompleteRelations {
        return $dbh->selectcol_arrayref("SELECT * FROM incomplete_relations");
    }

}
1;
