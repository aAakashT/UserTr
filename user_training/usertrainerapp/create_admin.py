import os
import sys
import django
from django.contrib.auth import get_user_model

sys.path.append(r'/home/neosoft/user_training/user_training')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'user_training.settings')
django.setup()
from models import User
from django.contrib.auth.models import  Group

def seed_admin():
    User = get_user_model()
    admin_group, _ = Group.objects.get_or_create(name='Admin')
    try:
        admin_user = User.objects.create_user(
            username='jia',
            password='Password@123',
            email='jia@gmail.com'
        )
    except Exception:    
        print("user with same email already exists")
    admin_user.groups.add(admin_group)
    print(f"user {admin_user} created sucessfully")
    return True
def run():
    admin  = seed_admin()

if __name__ == '__main__':
    run()
# not working
# model class models.user dosen't declare an explicit app_lable    