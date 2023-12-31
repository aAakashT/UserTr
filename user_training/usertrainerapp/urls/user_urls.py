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
from django.urls import path
from usertrainerapp.views.user_views import render_modules, user_dashboard_view, render_reviews, show_modules, show_reviews
from rest_framework.routers import DefaultRouter

urlpatterns = [
    path('api/user/modules/', render_modules, name='api_user_modules'),
    path('api/user/reviews/', render_reviews, name='api_user_reviews'),
    path('user/modules/', show_modules, name='show_modules'),
    path('user/reviews/', show_reviews, name='show_reviews'),
    path('user/dashboard/', user_dashboard_view, name='user_dashboard'),

]
