create table users (
   id  bigserial not null,
    created_at timestamp,
    email varchar(255) not null,
    facebook_id varchar(255),
    facebook_page_id varchar(255),
    facebook_token varchar(255),
    fb_access_token varchar(255),
    fb_token_expires_at timestamp,
    fb_user_id varchar(255),
    name varchar(255),
    updated_at timestamp,
    primary key (id)
);

create table campaigns (
   id  bigserial not null,
    budget float8,
    budget_type varchar(255),
    created_at timestamp,
    fb_campaign_id varchar(255),
    name varchar(255) not null,
    objective varchar(255),
    status varchar(255),
    updated_at timestamp,
    user_id int8 not null,
    primary key (id)
);

create table ads (
   id  bigserial not null,
    ad_type varchar(255),
    created_by varchar(255),
    created_date timestamp,
    call_to_action varchar(255),
    description varchar(500),
    fb_ad_id varchar(255),
    headline varchar(255),
    image_url varchar(255),
    name varchar(255),
    primary_text varchar(1000),
    status varchar(255),
    updated_by varchar(255),
    updated_date timestamp,
    campaign_id int8,
    primary key (id)
);

create table ad_contents (
   id  bigserial not null,
    ai_provider varchar(255),
    call_to_action varchar(255),
    content_type varchar(255),
    created_at timestamp,
    description varchar(500),
    headline varchar(255),
    image_url varchar(255),
    is_selected boolean,
    primary_text varchar(1000),
    updated_at timestamp,
    ad_id int8 not null,
    primary key (id)
);

create table lead_form_ads (
   id  bigserial not null,
    created_by varchar(255),
    created_date timestamp,
    fb_form_id varchar(255),
    form_name varchar(255),
    privacy_policy_url varchar(255),
    thanks_message varchar(255),
    updated_by varchar(255),
    updated_date timestamp,
    ad_id int8,
    primary key (id)
);

create table lead_form_fields (
   id  bigserial not null,
    created_by varchar(255),
    created_date timestamp,
    field_name varchar(255),
    field_type varchar(255),
    is_required boolean not null,
    updated_by varchar(255),
    updated_date timestamp,
    lead_form_ad_id int8,
    primary key (id)
);

create table page_post_ads (
   id  bigserial not null,
    created_by varchar(255),
    created_date timestamp,
    link_url varchar(255),
    page_id varchar(255),
    post_message varchar(255),
    post_type varchar(255),
    updated_by varchar(255),
    updated_date timestamp,
    ad_id int8,
    primary key (id)
);

create table website_conversion_ads (
   id  bigserial not null,
    created_by varchar(255),
    created_date timestamp,
    pixel_id varchar(255),
    updated_by varchar(255),
    updated_date timestamp,
    website_url varchar(255),
    ad_id int8,
    primary key (id)
);

create table website_conversion_events (
   website_conversion_ad_id int8 not null,
    conversion_events varchar(255)
);

alter table users
   add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email);

alter table users
   add constraint UK_jmubronqnn4q0cwe2egqsgvnl unique (facebook_id);

alter table users
   add constraint UK_7f7qpsjqee7i3tq2a3bit5tb2 unique (fb_user_id);

alter table campaigns
   add constraint FK9palj5bmfdaatu6s2d03oh7qe
   foreign key (user_id)
   references users;

alter table ads
   add constraint FKhiamvp74ed4jq67p3dqtd7peu
   foreign key (campaign_id)
   references campaigns;

alter table ad_contents
   add constraint FKg7koj562sc79sufu740rytre
   foreign key (ad_id)
   references ads;

alter table lead_form_ads
   add constraint FK1509rnk8vc8fpuw6ygovducp7
   foreign key (ad_id)
   references ads;

alter table lead_form_fields
   add constraint FKfqq8u7f7tqcit4617r2fu10lp
   foreign key (lead_form_ad_id)
   references lead_form_ads;

alter table page_post_ads
   add constraint FKdc4k7mc0irvgiqasc2cyyrb0a
   foreign key (ad_id)
   references ads;

alter table website_conversion_ads
   add constraint FK4p2dap2p7510yc7du7hod5wno
   foreign key (ad_id)
   references ads;

alter table website_conversion_events
   add constraint FK7lcffhhfjc8kvhnef8oeojxe6
   foreign key (website_conversion_ad_id)
   references website_conversion_ads;





 
 




