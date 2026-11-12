CREATE TABLE dbo.policies (
    id UNIQUEIDENTIFIER NOT NULL,
    policy_number VARCHAR(10) NOT NULL,
    policyholder_name NVARCHAR(255) NOT NULL,
    line_of_business VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    premium_amount DECIMAL(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    region NVARCHAR(50) NOT NULL,
    underwriter NVARCHAR(255) NOT NULL,
    flagged_for_review BIT NOT NULL
        CONSTRAINT DF_policies_flagged_for_review DEFAULT (0),
    created_at DATETIME2(7) NOT NULL,
    updated_at DATETIME2(7) NOT NULL,

    CONSTRAINT PK_policies PRIMARY KEY (id),
    CONSTRAINT UQ_policies_policy_number UNIQUE (policy_number),
    CONSTRAINT CK_policies_policy_number_format CHECK (
        LEN(policy_number) = 10
        AND policy_number LIKE 'POL-[0-9][0-9][0-9][0-9][0-9][0-9]'
    ),
    CONSTRAINT CK_policies_line_of_business CHECK (
        line_of_business IN ('Property', 'Casualty', 'A&H', 'Marine')
    ),
    CONSTRAINT CK_policies_status CHECK (
        status IN ('Active', 'Expired', 'Pending', 'Cancelled')
    ),
    CONSTRAINT CK_policies_premium_amount CHECK (
        premium_amount BETWEEN 1000 AND 5000000
    ),
    CONSTRAINT CK_policies_currency CHECK (
        currency IN ('USD', 'SGD', 'HKD', 'AUD', 'JPY', 'THB')
    ),
    CONSTRAINT CK_policies_region CHECK (
        region IN (
            N'Singapore',
            N'Hong Kong',
            N'Australia',
            N'Japan',
            N'Thailand',
            N'Indonesia',
            N'Malaysia',
            N'Philippines'
        )
    )
);
