from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    base_url: str
    timeout: float
    tenant_account: str | None
    tenant_password: str | None
    landlord_account: str | None
    landlord_password: str | None
    admin_account: str | None
    admin_password: str | None
    user_service_url: str | None
    house_service_url: str | None
    order_service_url: str | None
    payment_service_url: str | None
    contract_service_url: str | None
    notification_service_url: str | None
    test_contract_id: int | None

    @classmethod
    def from_env(cls) -> "Settings":
        base_url = os.getenv("API_BASE_URL", "").strip().rstrip("/")
        if not base_url:
            raise RuntimeError("API_BASE_URL is required")

        return cls(
            base_url=base_url,
            timeout=float(os.getenv("API_TIMEOUT", "10")),
            tenant_account=os.getenv("TEST_TENANT_ACCOUNT"),
            tenant_password=os.getenv("TEST_TENANT_PASSWORD"),
            landlord_account=os.getenv("TEST_LANDLORD_ACCOUNT"),
            landlord_password=os.getenv("TEST_LANDLORD_PASSWORD"),
            admin_account=os.getenv("TEST_ADMIN_ACCOUNT"),
            admin_password=os.getenv("TEST_ADMIN_PASSWORD"),
            user_service_url=os.getenv("USER_SERVICE_URL"),
            house_service_url=os.getenv("HOUSE_SERVICE_URL"),
            order_service_url=os.getenv("ORDER_SERVICE_URL"),
            payment_service_url=os.getenv("PAYMENT_SERVICE_URL"),
            contract_service_url=os.getenv("CONTRACT_SERVICE_URL"),
            notification_service_url=os.getenv("NOTIFICATION_SERVICE_URL"),
            test_contract_id=int(os.environ["TEST_CONTRACT_ID"]) if os.getenv("TEST_CONTRACT_ID") else None,
        )
