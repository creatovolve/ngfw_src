{
    "category": "Spam Blocker Lite",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        },
        {
            "column": "spam_blocker_lite_action",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","s_server_addr","addr","sender","subject","spam_blocker_lite_is_spam","spam_blocker_lite_action","spam_blocker_lite_score","spam_blocker_lite_tests_string"],
    "description": "All emails scanned by Spam Blocker.",
    "displayOrder": 1010,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "All Email Events",
    "uniqueId": "spam-blocker-lite-HEU9QMHY5S"
}
