"""
URL configuration for user_training project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/4.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path, include
from usertrainerapp.views.auth_views import UserRegistration, LoginApi, LogoutApi, login_view, register_view

from rest_framework.routers import DefaultRouter

# router = DefaultRouter()
# router.register(r'api/register', UserRegistration,basename="register")
# # for i in router.urls:
# #     print(i)
# app_name = 'auth'


urlpatterns = [
    path('api/login/', LoginApi.as_view(), name='login_api'),
    path('api/logout/', LogoutApi.as_view(), name='logout_api'),
    path('api/register', UserRegistration.as_view({'post':'create'}), name="register"),
    path('login/', login_view, name='login'),
    path('register/user/', register_view, name='register_user'),
]
