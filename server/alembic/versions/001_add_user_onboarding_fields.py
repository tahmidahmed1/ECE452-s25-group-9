"""Add user onboarding and profile fields

Revision ID: 001_add_user_onboarding_fields
Revises: 
Create Date: 2024-01-01 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '001_add_user_onboarding_fields'
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    # Create the user type enum
    user_type_enum = sa.Enum('VOLUNTEER', 'ORGANIZER', 'INSTITUTION', name='usertype')
    user_type_enum.create(op.get_bind())
    
    # Create the institution name enum
    institution_name_enum = sa.Enum('Institution 1', 'Institution 2', 'Institution 3', name='institutionname')
    institution_name_enum.create(op.get_bind())
    
    # Add new columns to users table
    op.add_column('users', sa.Column('user_type', user_type_enum, nullable=True))
    op.add_column('users', sa.Column('onboarding_completed', sa.Boolean(), default=False, nullable=False))
    op.add_column('users', sa.Column('full_name', sa.String(), nullable=True))
    op.add_column('users', sa.Column('phone', sa.String(), nullable=True))
    op.add_column('users', sa.Column('organization_name', sa.String(), nullable=True))
    op.add_column('users', sa.Column('institution_name', institution_name_enum, nullable=True))


def downgrade():
    # Remove columns
    op.drop_column('users', 'institution_name')
    op.drop_column('users', 'organization_name')
    op.drop_column('users', 'phone')
    op.drop_column('users', 'full_name')
    op.drop_column('users', 'onboarding_completed')
    op.drop_column('users', 'user_type')
    
    # Drop enums
    op.execute('DROP TYPE IF EXISTS institutionname')
    op.execute('DROP TYPE IF EXISTS usertype') 