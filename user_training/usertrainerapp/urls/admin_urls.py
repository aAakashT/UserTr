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
from usertrainerapp.views.admin_views import TrainingCreateView, UserListView, TrainingListView,AssignTLView, AssignRoleView, show_training_modules, show_users

app_name = 'admin2'
app = 'home'
url = "hi"
# path = "/"
# migrations = ""
template_name = ""




urlpatterns = [
    path('api/training/create/', TrainingCreateView.as_view(), name='api_training_create'),
    path('api/user/list/', UserListView.as_view(), name='api_user_list'),
    path('api/training/list/', TrainingListView.as_view(), name='api_training_list'),
    path('api/assign/tl/<int:user_id>/', AssignTLView.as_view(), name='api_assign_tl'),
    path('api/assign/role/<int:user_id>/', AssignRoleView.as_view(), name='api_assign_role'),

    path('training/modules/', show_training_modules, name='training_modules'),
    path('show/users/', show_users, name='show_users'),
]
