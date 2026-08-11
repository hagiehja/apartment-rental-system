from datetime import datetime, timedelta
import random
import uuid


def _suffix() -> str:
    return uuid.uuid4().hex[:8]


def new_user(role: str) -> dict:
    number = random.randint(0, 99_999_999)
    return {
        "username": f"api_{role.lower()}_{_suffix()}",
        "phone": f"199{number:08d}",
        "password": "ApiTest123",
        "role": role,
    }


def new_house() -> dict:
    suffix = _suffix()
    return {
        "title": f"接口自动化测试房源_{suffix}",
        "description": "pytest 创建，可在测试结束后删除",
        "province": "北京市",
        "city": "北京市",
        "district": "朝阳区",
        "address": f"测试路{suffix}号",
        "area": 60,
        "roomCount": 2,
        "hallCount": 1,
        "bathroomCount": 1,
        "floor": 5,
        "totalFloor": 18,
        "orientation": "SOUTH",
        "decoration": "FINE",
        "rentType": "WHOLE",
        "price": 3500,
        "paymentMethod": "MONTHLY",
        "facilities": ["WIFI", "AIR_CONDITIONER"],
        "imageUrls": [],
        "coverImageIndex": 0,
    }


def new_order(house_id: int) -> dict:
    return {
        "houseId": house_id,
        "rentStartDate": (datetime.now().date() + timedelta(days=7)).isoformat(),
        "rentMonths": 3,
        "installmentEnabled": False,
        "remark": "pytest e2e",
    }
