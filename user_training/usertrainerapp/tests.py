from django.test import TestCase
import os
import sys
import django
from django.db.models import Q
sys.path.append(r'/home/neosoft/user_training/user_training')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'user_training.settings')
django.setup()
# from django.contrib.auth.models import Permission
# # Create your tests here.

# print(Permission.objects.all()      )
# from rest_framework import permissions
# print(permissions.)
