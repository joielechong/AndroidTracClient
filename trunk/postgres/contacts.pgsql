drop trigger af_contact_del on contacts;
drop trigger af_contact_updtime on contacts;
drop trigger af_naw_upd on naw;
drop trigger af_cat_upd on catcontact;
drop trigger af_fax_upd on fax;
drop trigger af_tel_upd on telephone;
drop trigger af_mail_upd on mail;
drop trigger af_rel_upd on relaties;
drop trigger af_naw_del on naw;
drop trigger af_cat_del on catcontact;
drop trigger af_fax_del on fax;
drop trigger af_tel_del on telephone;
drop trigger af_mail_del on mail;
drop trigger af_rel_del on relaties;

create or replace function af_contact_del() returns trigger as
'begin
	delete from naw where contact_id=old.id;
	delete from catcontact where contact_id=old.id;
	delete from fax where contact_id=old.id;
	delete from telephone where contact_id=old.id;
	delete from mail where contact_id=old.id;
	delete from relaties where contact_id=old.id;
	update relaties set relatie_id = null where relatie_id=old.id;
return old;
end;' language 'plpgsql';

create trigger af_contact_del before delete
	on contacts for each row execute procedure af_contact_del();

CREATE or replace FUNCTION af_contact_updtime() RETURNS "trigger"
    AS 'begin
  new.updatetime=now();
  return new;
end;'
    LANGUAGE 'plpgsql';

CREATE or replace FUNCTION af_updtime() RETURNS "trigger"
    AS 'begin
  update contacts set updatetime=now() where id=new.contact_id;
  return new;
end;'
    LANGUAGE 'plpgsql';

CREATE or replace FUNCTION af_updtime1() RETURNS "trigger"
    AS 'begin
  update contacts set updatetime=now() where id=old.contact_id;
  return old;
end;'
    LANGUAGE 'plpgsql';

CREATE TRIGGER af_contact_updtime
    BEFORE INSERT OR UPDATE ON contacts
    FOR EACH ROW
    EXECUTE PROCEDURE af_contact_updtime();

create trigger af_naw_upd before insert or update on naw for each row 
  execute procedure af_updtime();

create trigger af_cat_upd before insert or update on catcontact for each row 
  execute procedure af_updtime();

create trigger af_fax_upd before insert or update on fax for each row 
  execute procedure af_updtime();

create trigger af_tel_upd before insert or update on telephone for each row 
  execute procedure af_updtime();

create trigger af_mail_upd before insert or update on mail for each row 
  execute procedure af_updtime();

create trigger af_rel_upd before insert or update on relaties for each row 
  execute procedure af_updtime();

create trigger af_naw_del before delete on naw for each row 
  execute procedure af_updtime1();

create trigger af_cat_del before delete on catcontact for each row 
  execute procedure af_updtime1();

create trigger af_fax_del before delete on fax for each row 
  execute procedure af_updtime1();

create trigger af_tel_del before delete on telephone for each row 
  execute procedure af_updtime1();

create trigger af_mail_del before delete on mail for each row 
  execute procedure af_updtime1();

create trigger af_rel_del before delete on relaties for each row 
  execute procedure af_updtime1();

