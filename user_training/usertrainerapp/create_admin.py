import os
import sys
import django
from django.db.models import Q
sys.path.append(r'/home/neosoft/user_training/user_training')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'user_training.settings')
django.setup()
from django.contrib.auth.models import  Group
from django.core.management.base import BaseCommand
from usertrainerapp.models import User

class Command(BaseCommand):
    def handle(self, *args, **options):
        self.create_admins()

    def create_admins(self):
        admin_group, created = Group.objects.get_or_create(name='admin')
        if not User.objects.filter(groups=admin_group).exists():
            admin_user = User.objects.create_user(
                username='jia',
                password='admin_password',
                email='admin@example.com'
            )
            admin_user.groups.add(admin_group)
            self.stdout.write('Admin user created successfully.')
        else:
            self.stdout.write('Admin user already exists.')
