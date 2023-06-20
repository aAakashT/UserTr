import os
import sys
import django
from django.db.models import Q
sys.path.append(r'/home/neosoft/user_training/user_training')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'user_training.settings')
django.setup()
from django.contrib.auth.models import Group   # always import Group after setting module
def seed_roles():
    """create roles and then check if they are exists or not"""
    admin_role = 'Admin'
    team_leader_role = 'Team_Leader'
    user_role = 'User'
    admin, created = Group.objects.get_or_create(name=admin_role)
    if created:
        print(f"Role '{admin_role}' created successfully.")
    else:
        print(f"Role '{admin_role}' already exists.")
    tl, created = Group.objects.get_or_create(name=team_leader_role)
    if created:
        print(f"Role '{team_leader_role}' created successfully.")
    else:
        print(f"Role '{team_leader_role}' already exists.")
    user, created = Group.objects.get_or_create(name=user_role)
    if created:
        print(f"Role '{user_role}' created successfully.")
    else:
        print(f"Role '{user_role}' already exists.")
    # if any new role is created then it will be automatically deleted    
    Group.objects.filter(~Q(name=team_leader_role) & ~Q(name=admin_role) &~Q(name=user_role)).delete()
    return admin, tl, user

def run():
    admin, tl, user = seed_roles()
    print(admin, tl, user)

if __name__ == '__main__':
    run()