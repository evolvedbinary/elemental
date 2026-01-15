# LDAP Security Realm for Elemental

The LDAP Security Realm allows Elemental to obtain authentication and user and group information from LDAP.

To enable the LDAP Security Realm, you need to make sure that the security configuration `/db/system/security/config.xml` within
the database contains a definition for th LDAP Realm and configuration that matches your LDAP server's schemas.

## NIS Example

The following is an example of an LDAP Security Realm configuration for connecting to an LDAP Server that provides the NIS Schema.
This has been tested with [Apache Directory Server](https://directory.apache.org/).

```xml
<security-manager xmlns="http://exist-db.org/Configuration">

    ...

    <realm id="LDAP" version="1.0" principals-are-case-insensitive="false">
        <context>
            <authentication>simple</authentication>
            <use-ssl>true</use-ssl>
            <url>ldaps://gb.myorg.com:389</url>
            <domain>gb.myorg.com</domain>
            <principal-pattern>uid={0},ou=Users,dc=gb,dc=myorg,dc=com</principal-pattern>
            <search>
                <base>dc=gb,dc=myorg,dc=com</base>
                <default-username>some-username</default-username>
                <default-password>some-password</default-password>

                <account>
                    <search-filter-prefix>objectClass=inetOrgPerson</search-filter-prefix>
                    <search-attribute key="objectSid">uidNumber</search-attribute>
                    <search-attribute key="name">uid</search-attribute>
                    <search-attribute key="dn">uid</search-attribute>
                    <search-attribute key="primaryGroupID">gidNumber</search-attribute>

                    <metadata-search-attribute key="http://axschema.org/namePerson/first">givenName</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/namePerson/last">sn</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/namePerson">displayName</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/pref/language">preferredLanguage</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/contact/email">mail</metadata-search-attribute>
                </account>

                <group>
                    <search-filter-prefix>objectClass=posixGroup</search-filter-prefix>
                    <search-attribute key="objectSid">gidNumber</search-attribute>
                    <search-attribute key="name">cn</search-attribute>
                    <search-attribute key="member">memberUid</search-attribute>

                    <metadata-search-attribute key="http://exist-db.org/security/description">description</metadata-search-attribute>

                    <whitelist>
                        <principal>some-other-group-1</principal>
                        <principal>some-other-group-2</principal>
                    </whitelist>
                </group>
            </search>

            <transformation>
                <add-group>ldap-users</add-group>
            </transformation>

        </context>
    </realm>

</security-manager>
```

## Microsoft Active Directory Example

The following is an example of an LDAP Security Realm configuration for connecting to Microsoft Active Directory.

```xml
<security-manager xmlns="http://exist-db.org/Configuration">

    ...

    <realm id="LDAP" version="1.0" principals-are-case-insensitive="true">
        <context>
            <authentication>simple</authentication>
            <use-ssl>true</use-ssl>
            <principal-pattern>sAMAccountName={0},ou=Users,dc=gb,dc=myorg,dc=com</principal-pattern>
            <url>ldaps://gb.myorg.com:636</url>
            <domain>gb.myorg.com</domain>
            <search>
                <base>dc=gb,dc=myorg,dc=com</base>
                <default-username>some-username</default-username>
                <default-password>some-password</default-password>

                <account>
                    <search-filter-prefix>(&amp;(objectClass=user)(memberof=cn=some-group,ou=Groups,dc=gb,dc=myorg,dc=com))</search-filter-prefix>
                    <search-attribute key="objectSid">objectSid</search-attribute>
                    <search-attribute key="name">sAMAccountName</search-attribute>
                    <search-attribute key="dn">distinguishedName</search-attribute>
                    <search-attribute key="primaryGroupID">primaryGroupID</search-attribute>
                    <search-attribute key="memberOf">memberOf</search-attribute>

                    <metadata-search-attribute key="http://axschema.org/contact/email">mail</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/namePerson/first">givenName</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/namePerson/last">sn</metadata-search-attribute>
                    <metadata-search-attribute key="http://axschema.org/namePerson">name</metadata-search-attribute>
                </account>

                <group>
                    <search-filter-prefix>objectClass=group</search-filter-prefix>
                    <search-attribute key="objectSid">objectSid</search-attribute>
                    <search-attribute key="name">sAMAccountName</search-attribute>
                    <search-attribute key="dn">distinguishedName</search-attribute>
                    <search-attribute key="member">member</search-attribute>

                    <metadata-search-attribute key="http://exist-db.org/security/description">description</metadata-search-attribute>

                    <whitelist>
                        <principal>Domain Users</principal>
                        <principal>some-group</principal>
                    </whitelist>
                </group>
            </search>

            <transformation>
                <add-group>ad-users</add-group>
            </transformation>

        </context>
    </realm>

</security-manager>
```